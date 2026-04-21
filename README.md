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