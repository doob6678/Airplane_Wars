# AircraftWar

Android 版飞机大战。

## 项目结构

- `app/`: Android 主工程（唯一参与当前构建与运行的模块）
- `app/src/main/`: Android 应用源码与资源
- `app/src/test/` 和 `app/src/androidTest/`: 单元测试与仪器测试
- `legacy-desktop/`: 早期 Swing/AWT 桌面版代码归档，不参与 Android 构建

# 联网排行榜功能
- 实现玩家之间的联网排行榜功能，包括玩家排名、积分、对战记录等。
- 支持玩家在不同设备上进行游戏，实时更新排行榜。
- 提供玩家之间的对战记录查询功能，包括对战时间、对战结果、对战对手等。
- 支持玩家在不同设备上进行游戏战，实现跨设备游戏。

## 联网排行版
不支持删除排名
最多显示10个
会合并同一个userid


# 我的启动
# 方式1：先进入后端模块目录再启动
cd C:\Users\wsdsb\Desktop\school_project\soft_build_2026\code_reinit\SocketServer\SocketServer
javac -encoding UTF-8 src\main\java\com\example\socketserver\MyClass.java
java -cp src\main\java com.example.socketserver.MyClass

---

## SocketServer 与联机排行榜设计说明

### 1. 目标与边界

- 本地模式（简单/普通/困难）：
  - 排行榜仅使用本地 SQLite；
  - 按每次游戏成绩入库；
  - 支持“长按删除单条”和“清空全部”。
- 联机模式：
  - 通过 Socket 服务端进行匹配与对战状态同步；
  - 排行榜数据来自服务器请求；
  - 客户端仅展示，不删除服务器数据。

### 2. 模块职责

- Android 客户端
  - `MainActivity`：输入 `userId`、选择模式、携带参数进入 `GameActivity`。
  - `OnlineGame`：联机协议收发，匹配后开战，双端结束后触发联机结算。
  - `LeaderboardActivity`：
    - 本地模式：从 SQLite 读写与删除；
    - 联机模式：请求服务端 `GET_RANKING` 并展示。
- SocketServer（`MyClass`）
  - 监听 `9999` 端口；
  - 接收 `JOIN` 并做 FIFO 匹配；
  - 仅同一房间内转发 `SCORE/GAME_OVER`；
  - 对局结束后落盘 `rankings.json`；
  - 对 `GET_RANKING` 返回 Top10 JSON。

### 3. 通信协议（行文本）

- Client -> Server
  - `JOIN:<userId>`：进入匹配队列
  - `SCORE:<value>`：上报当前分数
  - `GAME_OVER`：上报本端结束
  - `GET_RANKING`：请求排行榜（短连接）
  - `bye`：主动断开
- Server -> Client
  - `INFO:WAITING`：等待匹配中
  - `MATCH:<opponentUserId>`：匹配成功
  - `OPP_SCORE:<value>`：对手分数更新
  - `OPP_GAME_OVER`：对手结束
  - `ERROR:<message>`：协议错误

### 4. 联机对战流程

1. 客户端连接服务端并发送 `JOIN:userId`。
2. 服务端将玩家放入等待队列；若队列已有玩家则立即配对。
3. 双方收到 `MATCH` 后，`OnlineGame` 才开始推进游戏逻辑（未匹配前暂停逻辑更新）。
4. 游戏中客户端按分数变化发送 `SCORE`，服务端仅转发给房间内对手。
5. 任一方发送 `GAME_OVER`，服务端通知另一方 `OPP_GAME_OVER`。
6. 双方都结束后，客户端进入结算；服务端将对局结果写入 `rankings.json`。

### 5. 排行榜规则

- 联机排行榜（服务端）
  - 合并同一 `userId`，只保留最高分；
  - 同分时取最近成绩；
  - 返回并展示 Top10。
- 本地排行榜（单机）
  - 保留每次成绩记录；
  - 不做 `userId` 合并；
  - 支持删除与清空。

### 6. 线程安全与异常处理

- 服务端匹配队列用 `MATCH_LOCK` 保护，避免并发匹配冲突。
- 排行榜内存与文件写入用 `RANK_LOCK` 保护，避免并发写文件损坏。
- `userId` 做标准化：空值回退、非法字符替换、最大长度限制，防止协议污染。
- 解析消息和分数时做异常捕获，防止单条坏消息拖垮连接线程。
- 客户端网络异常时降级显示（如“连接失败”），不阻塞主线程 UI。

### 7. 关键代码入口

- 服务端入口：`SocketServer/SocketServer/src/main/java/com/example/socketserver/MyClass.java`
- 联机游戏逻辑：`app/src/main/java/edu/hitsz/application/OnlineGame.java`
- 排行榜页面：`app/src/main/java/edu/hitsz/LeaderboardActivity.java`
- 本地 DAO：`app/src/main/java/edu/hitsz/dao/SQLiteScoreDaoImpl.java`
