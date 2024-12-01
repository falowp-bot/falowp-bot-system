![maven](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)
![maven](https://img.shields.io/badge/Ktor-3.0.0-a.svg)
![maven](https://img.shields.io/badge/go--cqhttp-1.2.0-red)
![maven](https://img.shields.io/badge/qq-bot-red)

# 小花落Bot

> 此项目基于qqBot和go-cqhttp开发的机器人框架

## 当前最新版本

```
// https://mvnrepository.com/artifact/com.blr19c.falowp/falowp-bot-system
implementation("com.blr19c.falowp:falowp-bot-system:1.4.3")
```

## [查看文档](https://falowp.blr19c.com)

## 更新日志

### 2.0.0-RC1/2/3/4/5/6(不向下兼容)

* 更新一些依赖版本
* 重做了协议适配器、文本检测、翻译、数据源、MinIO
* `ImageUrl`支持自定义注册
* 修复了一些已知问题
* 支持语音消息
* 支持进退群事件
* 支持撤回消息事件
* 优化了截图分辨率和字体渲染
* 配置文件由conf类型改为yaml
* 修复WebClient可能出现死锁的问题

### 1.4.3

* 优化了ImageUrl中获取摘要的逻辑
* 更新一些依赖版本

### 1.4.2

* 修复了队列消息无法正常处理顺序的问题
* `ChannelQueue`和`queueMessage`的默认队列大小从0改为无限

### 1.4.1

* 更新一些依赖版本
* 精简了utils中内容
* 优化了ImageUrl中获取摘要的逻辑
* 使用github-workflows发布

### 1.4.0(有内容不向下兼容)

* 修复message队列引起的不能重复获取消息问题
* 修复Webdriver没有逐级关闭导致残留chrome进程的问题
* 新增了在`BotApi.sendGroup`和`BotApi.sendPrivate`方法中指定接受人的参数
* 移除了对`SchedulingBotApi.addReceive`的支持（改为BotApi使用指定接受人的方式发送）

### 1.3.1

* 优化HookJoinPoint中BotApi的获取逻辑

### 1.3.0(有内容不向下兼容)

* 支持队列消息
* 支持获取引用消息内容
* 发送消息使用链式处理(自定义消息顺序) 注意: 链式消息取代了之前的消息并且不向下兼容
* 将之前的image扩展移动至了expand扩展,并新增了一些扩展(不兼容原路径)
* 更新一些依赖
* 更新默认的useragent
* 更新一些描述

### 1.2.6

* 修复(go-cqhttp下)群昵称为空字符时无法获取昵称的问题
* 更新一些依赖的版本

### 1.2.5

* 修复任务中使用`SchedulingBotApi.sendAllGroup`会调用未启用的适配器的问题

### 1.2.4

* 修复`帮助功能`tag中没有需要展示的内容时会存在一个空tag的问题

### 1.2.3

* 修复多插件-插件内配置文件有不生效问题

### 1.2.2

* 优化启动配置
* 翻译和文本验证判断空字符
* go-cqhttp发送失败时重试2次
* 更新一些依赖版本

### 1.2.1

* 插件更新

### 1.2.0

* hook增加等待消息
* 新增一些系统内置hook

### 1.1.0

* java17 -> java21

### 1.0.1

* 支持gq-cqhttp和qqBot
