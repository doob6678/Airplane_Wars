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
    // 对手 userId 在 MATCH 消息后更新；初始用于 UI“匹配中”提示。
    private volatile String opponentUserId = "匹配中";
    // 只有 matched=true 才允许游戏推进，确保双方在同一逻辑起点开局。
    private volatile boolean matched = false;
    private int opponentScore = 0;
    private boolean opponentGameOver = false;
    private boolean myGameOver = false;
    private int lastReportedScore = -1;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter writer;
    private Thread networkThread;

    /**
     * 创建联机模式游戏实例并发起服务器连接。
     *
     * @param context Android 上下文
     * @param userId  当前玩家 userId
     */
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
                // 连接建立后立刻声明身份，进入服务端匹配队列。
                writer.println("JOIN:" + userId);

                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    processServerMessage(fromServer);
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error", e);
                // 用可视文案反馈网络失败状态，避免玩家误以为卡死。
                opponentUserId = "连接失败";
            }
        }, "online-network-thread");
        networkThread.start();
    }

    /**
     * 解析服务端消息并更新本地联机状态。
     *
     * @param msg 一行协议消息
     */
    private void processServerMessage(String msg) {
        // 约定：收到 MATCH 才算“开始对局”，未匹配时 updateGame 直接 return。
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
        // 等待匹配阶段不推进游戏逻辑，避免“单人先开局”导致双方状态不一致。
        if (!matched) {
            return;
        }

        if (score != lastReportedScore && writer != null) {
            lastReportedScore = score;
            // 分数变化异步上报，避免阻塞主游戏循环帧率。
            new Thread(() -> {
                if (writer != null) {
                    writer.println("SCORE:" + score);
                }
            }).start();
        }

        if (heroAircraft == null || heroAircraft.notValid()) {
            if (!myGameOver) {
                myGameOver = true;
                // 仅首帧发送 GAME_OVER，避免重复刷消息。
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
        // 联机结算门槛：双方都结束才触发结算与跳转。
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
                // 主动结束后关闭 socket，释放 fd 与网络资源。
                closeSocket();
            }).start();
        }
    }

    /**
     * 关闭联机 Socket 连接。
     * 该方法仅处理关闭动作，不做重连策略。
     */
    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }

    /**
     * 渲染联机模式画面：
     * - 常规战斗元素；
     * - 本方/对方分数；
     * - 匹配状态提示。
     */
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
                // 明确告知“尚未匹配完成”，避免玩家误判为帧卡顿。
                canvas.drawText("MATCHING...", viewportLeft + 20, viewportTop + 230, paint);
            }
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * 规范化 userId，防止空值、非法字符与超长输入。
     *
     * @param raw 原始 userId
     * @return 安全可传输的 userId
     */
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
