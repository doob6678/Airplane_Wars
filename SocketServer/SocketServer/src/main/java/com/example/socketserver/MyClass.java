package com.example.socketserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Socket 服务端主类：
 * 1. 通过 JOIN:userId 进行联机匹配；
 * 2. 匹配成功后仅在同房间内转发双方分数/结束状态；
 * 3. 将每局结果写入 rankings.json；
 * 4. 通过 GET_RANKING 返回最新排行榜 JSON 数组。
 */
public class MyClass {
    private static final int SERVER_PORT = 9999;
    private static final int MAX_USER_ID_LENGTH = 24;
    private static final Object MATCH_LOCK = new Object();
    private static final Object RANK_LOCK = new Object();
    private static final Deque<ClientHandler> WAITING_QUEUE = new ArrayDeque<>();
    private static final List<RankingEntry> RANKINGS = new ArrayList<>();
    private static final File RANK_FILE = new File("rankings.json");

    public static void main(String[] args) {
        loadRankingsFromDisk();
        new MyClass();
    }

    public MyClass() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Online Game Server started. Waiting for clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void loadRankingsFromDisk() {
        synchronized (RANK_LOCK) {
            RANKINGS.clear();
            if (!RANK_FILE.exists()) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(RANK_FILE, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    RankingEntry entry = RankingEntry.fromJsonLine(line);
                    if (entry != null) {
                        RANKINGS.add(entry);
                    }
                }
            } catch (Exception ex) {
                System.out.println("Failed to load rankings.json, start with empty ranking list.");
            }
        }
    }

    private static void appendRanking(RankingEntry entry) {
        synchronized (RANK_LOCK) {
            RANKINGS.add(entry);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(RANK_FILE, StandardCharsets.UTF_8, true))) {
                writer.write(entry.toJsonLine());
                writer.newLine();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String buildRankingJson() {
        List<RankingEntry> snapshot;
        synchronized (RANK_LOCK) {
            snapshot = new ArrayList<>(RANKINGS);
        }

        // Merge same userId by highest score. If score ties, keep the latest record.
        Map<String, RankingEntry> bestByUser = new HashMap<>();
        for (RankingEntry entry : snapshot) {
            RankingEntry existing = bestByUser.get(entry.userId);
            if (existing == null
                    || entry.score > existing.score
                    || (entry.score == existing.score && entry.time > existing.time)) {
                bestByUser.put(entry.userId, entry);
            }
        }

        List<RankingEntry> merged = new ArrayList<>(bestByUser.values());
        merged.sort(Comparator.comparingInt((RankingEntry e) -> e.score).reversed()
                .thenComparingLong(e -> e.time));

        int limit = Math.min(merged.size(), 10);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < limit; i++) {
            RankingEntry e = merged.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"userId\":\"").append(e.userId)
                    .append("\",\"score\":").append(e.score)
                    .append(",\"time\":").append(e.time)
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String normalizeUserId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "Anonymous";
        }
        value = value.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (value.isEmpty()) {
            return "Anonymous";
        }
        if (value.length() > MAX_USER_ID_LENGTH) {
            return value.substring(0, MAX_USER_ID_LENGTH);
        }
        return value;
    }

    private static class MatchSession {
        private final ClientHandler left;
        private final ClientHandler right;
        private volatile boolean resultSaved = false;

        MatchSession(ClientHandler left, ClientHandler right) {
            this.left = left;
            this.right = right;
        }

        ClientHandler other(ClientHandler self) {
            return self == left ? right : left;
        }

        void forwardScore(ClientHandler sender, int score) {
            ClientHandler other = other(sender);
            if (other != null) {
                other.sendMessage("OPP_SCORE:" + score);
            }
        }

        void onGameOver(ClientHandler sender) {
            ClientHandler other = other(sender);
            if (other != null) {
                other.sendMessage("OPP_GAME_OVER");
            }
            maybeSaveResult();
        }

        synchronized void maybeSaveResult() {
            if (resultSaved) {
                return;
            }
            if (!left.gameOver || !right.gameOver) {
                return;
            }
            resultSaved = true;
            long now = System.currentTimeMillis();
            appendRanking(new RankingEntry(left.userId, left.lastScore, right.userId, right.lastScore, now));
            appendRanking(new RankingEntry(right.userId, right.lastScore, left.userId, left.lastScore, now));
            System.out.println("Match saved: " + left.userId + "(" + left.lastScore + ") vs "
                    + right.userId + "(" + right.lastScore + ")");
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private volatile String userId = "Anonymous";
        private volatile int lastScore = 0;
        private volatile boolean gameOver = false;
        private volatile MatchSession session;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);

                String firstLine = in.readLine();
                if (firstLine == null) {
                    return;
                }

                if ("GET_RANKING".equals(firstLine)) {
                    out.println(buildRankingJson());
                    return;
                }

                if (!firstLine.startsWith("JOIN:")) {
                    out.println("ERROR:invalid_join");
                    return;
                }

                userId = normalizeUserId(firstLine.substring("JOIN:".length()));
                enqueueAndTryMatch(this);

                String line;
                while ((line = in.readLine()) != null) {
                    if ("bye".equalsIgnoreCase(line)) {
                        break;
                    }
                    if (line.startsWith("SCORE:")) {
                        try {
                            int score = Integer.parseInt(line.substring("SCORE:".length()).trim());
                            if (score < 0) {
                                score = 0;
                            }
                            this.lastScore = score;
                            MatchSession current = session;
                            if (current != null) {
                                current.forwardScore(this, score);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    } else if ("GAME_OVER".equals(line)) {
                        this.gameOver = true;
                        MatchSession current = session;
                        if (current != null) {
                            current.onGameOver(this);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                removeFromWaitingQueue(this);
                closeQuietly();
            }
        }

        synchronized void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private void closeQuietly() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void enqueueAndTryMatch(ClientHandler current) {
        synchronized (MATCH_LOCK) {
            while (!WAITING_QUEUE.isEmpty()) {
                ClientHandler peer = WAITING_QUEUE.pollFirst();
                if (peer == null || peer.socket.isClosed() || peer == current) {
                    continue;
                }
                MatchSession session = new MatchSession(peer, current);
                peer.session = session;
                current.session = session;
                peer.sendMessage("MATCH:" + current.userId);
                current.sendMessage("MATCH:" + peer.userId);
                System.out.println("Matched: " + peer.userId + " <-> " + current.userId);
                return;
            }
            WAITING_QUEUE.offerLast(current);
            current.sendMessage("INFO:WAITING");
        }
    }

    private static void removeFromWaitingQueue(ClientHandler current) {
        synchronized (MATCH_LOCK) {
            WAITING_QUEUE.remove(current);
        }
    }

    private static class RankingEntry {
        private static final Pattern PATTERN = Pattern.compile(
                "\\{\"userId\":\"([^\"]+)\",\"score\":(\\d+),\"opponentUserId\":\"([^\"]+)\",\"opponentScore\":(\\d+),\"time\":(\\d+)\\}");
        final String userId;
        final int score;
        final String opponentUserId;
        final int opponentScore;
        final long time;

        RankingEntry(String userId, int score, String opponentUserId, int opponentScore, long time) {
            this.userId = normalizeUserId(userId);
            this.score = Math.max(0, score);
            this.opponentUserId = normalizeUserId(opponentUserId);
            this.opponentScore = Math.max(0, opponentScore);
            this.time = time <= 0 ? new Date().getTime() : time;
        }

        String toJsonLine() {
            return "{\"userId\":\"" + userId + "\",\"score\":" + score
                    + ",\"opponentUserId\":\"" + opponentUserId + "\",\"opponentScore\":" + opponentScore
                    + ",\"time\":" + time + "}";
        }

        static RankingEntry fromJsonLine(String line) {
            if (line == null) {
                return null;
            }
            Matcher m = PATTERN.matcher(line.trim());
            if (!m.matches()) {
                return null;
            }
            try {
                String userId = m.group(1);
                int score = Integer.parseInt(m.group(2));
                String opponentUserId = m.group(3);
                int opponentScore = Integer.parseInt(m.group(4));
                long time = Long.parseLong(m.group(5));
                return new RankingEntry(userId, score, opponentUserId, opponentScore, time);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
