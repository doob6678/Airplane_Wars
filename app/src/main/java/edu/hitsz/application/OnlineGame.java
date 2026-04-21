package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 联机模式游戏实现类。
 * 负责：
 * 1. 基于 userId 完成匹配。
 * 2. 匹配成功后再开始真正对战。
 * 3. 实时同步双方得分与结束状态。
 */
public class OnlineGame extends NormalGame {
    private static final String TAG = "OnlineGame";
    private static final String SERVER_HOST = "10.0.2.2";
    private static final int SERVER_PORT = 9999;
    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final String userId;
    private volatile String opponentUserId = "匹配中";
    private volatile boolean matched = false;
    private int opponentScore = 0;
    private boolean opponentGameOver = false;
    private boolean myGameOver = false;
    private int lastReportedScore = -1;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter writer;
    private Thread networkThread;

    public OnlineGame(Context context, String userId) {
        super(context);
        this.userId = normalizeUserId(userId);
        connectToServer();
    }

    /**
     * 连接至远程服务器端并初始化通信流。
     * 在后台线程中阻塞读取服务器发送的数据，以此同步对手分数和结束状态。
     */
    private void connectToServer() {
        networkThread = new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), CONNECT_TIMEOUT_MS);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
                writer.println("JOIN:" + userId);

                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    processServerMessage(fromServer);
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error", e);
                opponentUserId = "连接失败";
            }
        }, "online-network-thread");
        networkThread.start();
    }

    private void processServerMessage(String msg) {
        if (msg.startsWith("MATCH:")) {
            String matchedUser = msg.substring("MATCH:".length()).trim();
            if (!matchedUser.isEmpty()) {
                opponentUserId = matchedUser;
            } else {
                opponentUserId = "Unknown";
            }
            matched = true;
            return;
        }
        if (msg.startsWith("OPP_SCORE:")) {
            try {
                opponentScore = Integer.parseInt(msg.substring("OPP_SCORE:".length()).trim());
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid score from server: " + msg);
            }
            return;
        }
        if (msg.equals("OPP_GAME_OVER")) {
            opponentGameOver = true;
            checkMatchOver();
            return;
        }
        if (msg.startsWith("ERROR:")) {
            opponentUserId = msg.substring("ERROR:".length()).trim();
        }
    }

    /**
     * 判断当前联机对局是否双端均已结束。
     * 若自己和对手都已标记为 "OVER" 状态，则触发最终处理。
     */
    private void checkMatchOver() {
        if (myGameOver && opponentGameOver) {
            handleGameOverIfNeeded();
        }
    }

    /**
     * 更新游戏逻辑的同时同步本机的游戏状态至服务器。
     * 1. 监测分数变化并主动报告给服务器。
     * 2. 当英雄机坠毁时，发送终局状态。
     */
    @Override
    protected void updateGame() {
        if (!matched) {
            return;
        }

        if (score != lastReportedScore && writer != null) {
            lastReportedScore = score;
            new Thread(() -> {
                if (writer != null) {
                    writer.println("SCORE:" + score);
                }
            }).start();
        }

        if (heroAircraft == null || heroAircraft.notValid()) {
            if (!myGameOver) {
                myGameOver = true;
                new Thread(() -> {
                    if (writer != null) {
                        writer.println("GAME_OVER");
                    }
                }).start();
            }
            checkMatchOver();
            return;
        }

        super.updateGame();
    }

    @Override
    protected void handleGameOverIfNeeded() {
        // Suppress early game over in Online mode unless both are dead
        if (myGameOver && opponentGameOver) {
            if (gameOverHandled) {
                return;
            }
            gameOverHandled = true;
            if (audioManager != null) {
                audioManager.stopAllBgm();
                audioManager.playGameOver();
            }
            GameEventListener listener = gameEventListener;
            if (listener != null) {
                listener.onOnlineGameOver(score, opponentScore, opponentUserId);
            }
            new Thread(() -> {
                if (writer != null) {
                    writer.println("bye");
                }
                closeSocket();
            }).start();
        }
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }

    @Override
    protected void drawFrame() {
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            canvas.drawColor(Color.BLACK);

            if (backgroundBitmap != null && viewportWidth > 0 && viewportHeight > 0) {
                Rect dst = new Rect(
                        (int) viewportLeft,
                        (int) viewportTop,
                        (int) (viewportLeft + viewportWidth),
                        (int) (viewportTop + viewportHeight));
                canvas.drawBitmap(backgroundBitmap, null, dst, paint);
            }

            drawFlyingObjects(canvas, enemyBullets);
            drawFlyingObjects(canvas, heroBullets);
            drawFlyingObjects(canvas, enemyAircraft);
            drawFlyingObjects(canvas, props);

            if (heroAircraft != null && !heroAircraft.notValid()) {
                drawFlyingObject(canvas, heroAircraft, ImageManager.HERO_IMAGE);
            }

            paint.setColor(Color.RED);
            paint.setTextSize(36f);
            canvas.drawText("SCORE: " + score, viewportLeft + 20, viewportTop + 46, paint);
            canvas.drawText("OPPONENT: " + opponentUserId, viewportLeft + 20, viewportTop + 88, paint);
            canvas.drawText("ENEMY SCORE: " + opponentScore, viewportLeft + 20, viewportTop + 130, paint);
            if (heroAircraft != null) {
                canvas.drawText("LIFE: " + heroAircraft.getHp(), viewportLeft + 20, viewportTop + 172, paint);
            }

            if (!matched) {
                paint.setColor(Color.YELLOW);
                paint.setTextSize(42f);
                canvas.drawText("MATCHING...", viewportLeft + 20, viewportTop + 230, paint);
            }
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private String normalizeUserId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "匿名玩家";
        }
        value = value.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (value.isEmpty()) {
            return "匿名玩家";
        }
        return value.length() > 24 ? value.substring(0, 24) : value;
    }
}
