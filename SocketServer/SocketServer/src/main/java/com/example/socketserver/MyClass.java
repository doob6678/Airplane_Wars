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
 *
 * 纯文本协议（每条消息一行）：
 * - Client -> Server:
 *   JOIN:<userId>      申请进入匹配队列
 *   SCORE:<value>      上报当前分数
 *   GAME_OVER          上报本端结束
 *   GET_RANKING        请求在线排行榜（仅请求场景）
 *   bye                主动断开
 *
 * - Server -> Client:
 *   INFO:WAITING       当前在等待匹配
 *   MATCH:<userId>     匹配成功，对手 userId
 *   OPP_SCORE:<value>  对手分数更新
 *   OPP_GAME_OVER      对手结束
 *   ERROR:<message>    协议或参数错误
 */
public class MyClass {
    private static final int SERVER_PORT = 9999;
    private static final int MAX_USER_ID_LENGTH = 24;
    // MATCH_LOCK 仅保护匹配队列与会话建立过程，避免两个线程同时抢同一等待玩家。
    private static final Object MATCH_LOCK = new Object();
    // RANK_LOCK 保护排行榜内存列表与文件写，确保“内存与磁盘”状态一致。
    private static final Object RANK_LOCK = new Object();
    // FIFO 等待队列：先进入匹配队列的玩家优先配对。
    private static final Deque<ClientHandler> WAITING_QUEUE = new ArrayDeque<>();
    // 运行期内存排行榜（服务端全局），启动时从 rankings.json 恢复。
    private static final List<RankingEntry> RANKINGS = new ArrayList<>();
    private static final File RANK_FILE = new File("rankings.json");

    /**
     * 服务端进程入口。
     * 启动顺序：先恢复历史排行榜，再进入 Socket 监听循环。
     *
     * @param args 命令行参数（当前未使用）
     */
    public static void main(String[] args) {
        loadRankingsFromDisk();
        new MyClass();
    }

    /**
     * 构造并启动服务端监听循环。
     * 持续接收客户端连接并为每个连接创建独立处理线程。
     */
    public MyClass() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Online Game Server started. Waiting for clients...");

            while (true) {
                // 一连接一线程：协议简单、并发量较低时可快速实现，便于教学场景排错。
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 启动时恢复历史排行榜。
     * 文件格式为“JSON 行”，每行一个 RankingEntry，读失败不抛出中断服务。
     */
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

    /**
     * 追加一条记录到内存与磁盘。
     * 线程安全：通过 RANK_LOCK 串行化内存写 + 文件写，避免并发交错写坏文件。
     */
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

    /**
     * 构造对外排行榜 JSON：
     * 1) 对同 userId 按最高分合并；
     * 2) 分数相同取最近记录；
     * 3) 仅返回 Top10。
     */
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

    /**
     * 规范化 userId：
     * - 空值回退为 Anonymous；
     * - 过滤非法字符；
     * - 截断到最大长度，防止协议污染或超长字段。
     *
     * @param raw 客户端原始 userId
     * @return 规范化后的安全 userId
     */
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

        /**
         * 创建一场双人对局会话。
         *
         * @param left  左侧玩家
         * @param right 右侧玩家
         */
        MatchSession(ClientHandler left, ClientHandler right) {
            this.left = left;
            this.right = right;
        }

        /**
         * 由当前调用方定位会话中的对手对象。
         *
         * @param self 当前玩家
         * @return 对手玩家；若 self 非会话成员，默认返回 left 的对侧逻辑结果
         */
        ClientHandler other(ClientHandler self) {
            return self == left ? right : left;
        }

        /**
         * 将调用方的分数转发给对手。
         *
         * @param sender 分数发送方
         * @param score  最新分数
         */
        void forwardScore(ClientHandler sender, int score) {
            ClientHandler other = other(sender);
            if (other != null) {
                other.sendMessage("OPP_SCORE:" + score);
            }
        }

        /**
         * 处理某一方结束事件，并通知对手结束状态。
         *
         * @param sender 发送 GAME_OVER 的玩家
         */
        void onGameOver(ClientHandler sender) {
            ClientHandler other = other(sender);
            if (other != null) {
                other.sendMessage("OPP_GAME_OVER");
            }
            // 每收到一次 GAME_OVER 都尝试保存；真正落盘由 maybeSaveResult 的双端条件控制。
            maybeSaveResult();
        }

        /**
         * 双方都 GAME_OVER 后只落库一次，避免重复写入。
         * 对同一局写两条记录：A 视角一条、B 视角一条。
         */
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

        /**
         * 创建一个客户端连接处理器。
         *
         * @param socket 客户端 Socket
         */
        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * 连接线程主循环：
         * - 首包判定 GET_RANKING 或 JOIN；
         * - JOIN 后持续处理 SCORE/GAME_OVER/bye；
         * - 退出时统一清理等待队列与连接资源。
         */
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

                // 排行榜请求走“短连接”模式：请求一次，返回一次，立即结束。
                if ("GET_RANKING".equals(firstLine)) {
                    out.println(buildRankingJson());
                    return;
                }

                if (!firstLine.startsWith("JOIN:")) {
                    out.println("ERROR:invalid_join");
                    return;
                }

                // 联机对战走“长连接”模式：先 JOIN，再收发 SCORE/GAME_OVER。
                userId = normalizeUserId(firstLine.substring("JOIN:".length()));
                enqueueAndTryMatch(this);

                String line;
                while ((line = in.readLine()) != null) {
                    if ("bye".equalsIgnoreCase(line)) {
                        // 客户端主动退出，不再继续处理后续消息。
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
                                // 只在已匹配会话内转发，防止广播到无关连接。
                                current.forwardScore(this, score);
                            }
                        } catch (NumberFormatException ignored) {
                            // 非法分数字符串直接忽略，不中断连接线程。
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

        /**
         * 向当前连接发送一行协议消息。
         * 使用 synchronized 保证同一连接上的发送顺序稳定。
         *
         * @param message 待发送协议文本
         */
        synchronized void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        /**
         * 安静关闭连接，忽略关闭异常，避免影响收尾流程。
         */
        private void closeQuietly() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 匹配策略：FIFO。
     * - 队列中有等待玩家：立即与当前玩家配对；
     * - 队列为空：当前玩家入队等待。
     */
    private static void enqueueAndTryMatch(ClientHandler current) {
        synchronized (MATCH_LOCK) {
            while (!WAITING_QUEUE.isEmpty()) {
                ClientHandler peer = WAITING_QUEUE.pollFirst();
                if (peer == null || peer.socket.isClosed() || peer == current) {
                    // 过滤无效连接和异常情况，继续找下一位候选。
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
            // 没有可配对玩家时立刻回传等待态，客户端可以据此显示“匹配中”。
            current.sendMessage("INFO:WAITING");
        }
    }

    /**
     * 从等待队列删除某个客户端（断线/退出时调用）。
     *
     * @param current 当前客户端
     */
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

        /**
         * 创建一条排行榜记录。
         *
         * @param userId          当前玩家 ID
         * @param score           当前玩家分数
         * @param opponentUserId  对手 ID
         * @param opponentScore   对手分数
         * @param time            记录时间戳（毫秒）
         */
        RankingEntry(String userId, int score, String opponentUserId, int opponentScore, long time) {
            this.userId = normalizeUserId(userId);
            this.score = Math.max(0, score);
            this.opponentUserId = normalizeUserId(opponentUserId);
            this.opponentScore = Math.max(0, opponentScore);
            this.time = time <= 0 ? new Date().getTime() : time;
        }

        /**
         * 将记录编码为单行 JSON（JSON Lines 格式）。
         *
         * @return 可直接持久化的一行文本
         */
        String toJsonLine() {
            return "{\"userId\":\"" + userId + "\",\"score\":" + score
                    + ",\"opponentUserId\":\"" + opponentUserId + "\",\"opponentScore\":" + opponentScore
                    + ",\"time\":" + time + "}";
        }

        /**
         * 从单行 JSON 反序列化记录对象。
         *
         * @param line JSON 行文本
         * @return 成功返回对象，失败返回 null
         */
        static RankingEntry fromJsonLine(String line) {
            if (line == null) {
                return null;
            }
            Matcher m = PATTERN.matcher(line.trim());
            if (!m.matches()) {
                // 非法历史行直接跳过，避免历史脏数据导致服务启动失败。
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
