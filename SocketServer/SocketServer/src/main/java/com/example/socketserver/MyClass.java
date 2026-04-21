package com.example.socketserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Socket服务端主类，用于提供联机模式的对战同步。
 * 维护了一个客户端列表，并将一个客户端发送的积分/状态消息广播给其他所有客户端。
 */
public class MyClass {
    private static final List<Service> clients = new ArrayList<>();

    public static void main(String[] args) {
        new MyClass();
    }

    public MyClass() {
        try {
            // ① 创建ServerSocket， 指定端口号， 等待客户端连接
            ServerSocket serverSocket = new ServerSocket(9999);
            System.out.println("Online Game Server started. Waiting for clients...");

            while (true) {
                // ② 客户端连接成功后， 创建Socket对象， 启动子线程和客户端通信
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());
                Service service = new Service(socket);
                clients.add(service);
                new Thread(service).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 客户端服务线程。
     * 当新的客户端连接时实例化，分配独立线程以持续读取来自该客户端的数据，
     * 并把数据广播到其他处于连接状态的客户端（如分数同步与游戏结束通知）。
     */
    class Service implements Runnable {
        private Socket socket;
        private BufferedReader in = null;
        private PrintWriter pout = null;

        public Service(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                pout = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "utf-8")), true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            String content = "";
            try {
                // 接收客户端信息
                while ((content = in.readLine()) != null) {
                    if (content.equals("bye") || content.equals("GAME_OVER")) {
                        broadcastMessage(content, this);
                        if (content.equals("bye")) {
                            break;
                        }
                    } else if (content.startsWith("SCORE:")) {
                        // 任意一方分数发生变化时，另一方能实时看到更新后的分数
                        broadcastMessage(content, this);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                System.out.println("Disconnect from client, closing socket");
                clients.remove(this);
                // 通信结束后，关闭Socket连接（客户端或服务器均可发起关闭）
                try {
                    if (!socket.isClosed()) {
                        socket.shutdownInput();
                        socket.shutdownOutput();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 向本客户端发送字符串消息。
         *
         * @param message 待发送的内容文本
         */
        public void sendMessage(String message) {
            pout.println(message);
        }

        /**
         * 将消息内容广播给除发送源之外的所有其他客户端。
         * 主要用于转发对战中的分数与进度信息。
         *
         * @param message 发送的消息串 (如 "SCORE:...", "GAME_OVER")
         * @param sender  不需要收到本条消息的原始发送者服务实体
         */
        private void broadcastMessage(String message, Service sender) {
            for (Service client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }
}