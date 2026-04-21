package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 联机模式游戏实现类。
 * 继承自 {@link NormalGame}。
 * 负责通过 Socket 与服务器进行交互，包含同步玩家分数和监听对手游戏状态的逻辑。
 */
public class OnlineGame extends NormalGame {
    private static final String TAG = "OnlineGame";
    private int opponentScore = 0;
    private boolean opponentGameOver = false;
    private boolean myGameOver = false;
    private int lastReportedScore = -1;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter writer;
    private Thread networkThread;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public OnlineGame(Context context) {
        super(context);
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
                socket.connect(new InetSocketAddress("10.0.2.2", 9999), 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "utf-8")), true);

                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    processServerMessage(fromServer);
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error", e);
            }
            /**
             * 解析服务器广播回来的消息，并更新对方得分和状态。
             *
             * @param msg 按行读取的服务器字符串消息 ("SCORE:..." 或 "GAME_OVER")
             */
        });
        networkThread.start();
    }

    private void processServerMessage(String msg) {
        if (msg.startsWith("SCORE:")) {
            try {
                opponentScore = Integer.parseInt(msg.substring(6));
            } catch (NumberFormatException e) {
                // ignore
            }
        } else if (msg.equals("GAME_OVER")) {
            opponentGameOver = true;
            checkMatchOver();
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
                listener.onOnlineGameOver(score, opponentScore);
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
            canvas.drawText("ENEMY SCORE: " + opponentScore, viewportLeft + 20, viewportTop + 88, paint);
            if (heroAircraft != null) {
                canvas.drawText("LIFE: " + heroAircraft.getHp(), viewportLeft + 20, viewportTop + 130, paint);
            }
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
}