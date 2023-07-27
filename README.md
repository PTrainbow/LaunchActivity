# Application 阶段获取 Intent

目前通过在 Application onCreate 阶段，获取 MQ 中的 message 的 obj 来进行判定

# 原理

![](/doc/startActivity.png)

BindApplication 和 StartActivityLocked 在 AMS 进程，会顺序的发送两个消息到应用进程

只要应用在 Application onCreate 阶段，Activity 启动的消息已经收到即可

理论上存在获取失败的可能：比如 AMS 发送 Application 创建消息后，卡住，后续的 Activity 创建消息迟迟不发送，这样 MQ 获取不到对应 message 信息

message.obj 如果为空，兜底再取一个 next