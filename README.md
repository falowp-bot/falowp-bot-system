![maven](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)
![maven](https://img.shields.io/badge/Ktor-3.0.0-a.svg)
![maven](https://img.shields.io/badge/go--cqhttp-1.2.0-red)
![maven](https://img.shields.io/badge/qq-bot-red)

# 小花落 Bot

> 此项目基于QQBot和go-cqhttp开发的机器人框架

## 当前最新版本

```
// https://mvnrepository.com/artifact/com.blr19c.falowp/falowp-bot-system
implementation("com.blr19c.falowp:falowp-bot-system:2.2.4")
```

## [查看文档](https://falowp.blr19c.com)

## 更新日志

### 2.3.0(不向下兼容)

* 更新一些依赖版本
* 更新默认的`useragent`
* `java21` -> `java25`
* `jackson2` -> `jackson3`
* `mamoe-yaml` -> `jackson3-yaml`
* 移除了对图片的增强工具 `BufferedImage.kt`
* 移除了`gson`全面使用`jackson3`
* 移除了`netty`全面使用`CIO`
* 将戳一戳/拍一拍由消息类型更改为事件类型`NudgeEvent`
* `MessagePluginRegisterMatch`改为`MessageMatch`
* `ReceiveMessage`中`Self`信息改为`BotSelf`,并支持在`BotApi`中直接获取
* `Event`事件中增加来源和触发人
* 重做了[`plugin`](https://github.com/falowp-bot/falowp-bot-system/blob/main/src/main/kotlin/com/blr19c/falowp/bot/system/plugin/Plugin.kt)
    - 原`eventListener`移动到`com.blr19c.falowp.bot.system.plugin.event`
    - 原`hook、beforeHook、...、aroundHook`移动到`com.blr19c.falowp.bot.system.plugin.hook`
    - 原`message、queueMessage`移动到`com.blr19c.falowp.bot.system.plugin.message`
    - 原`cronScheduling、periodicScheduling、applicationInitScheduling`移动到`com.blr19c.falowp.bot.system.plugin.task`
    - 原`poke`移动到`com.blr19c.falowp.bot.system.plugin.event.nudgeMe`
* 由于（`gocq-http`、`AstralGocq`）停止维护，终止了对[`-cq`](https://github.com/falowp-bot/falowp-bot-plugins/tree/main/falowp-bot-adapter-cq)的维护，后续版本中可能会删除此适配器
* 重做了[`-nc`](https://github.com/falowp-bot/falowp-bot-plugins/tree/main/falowp-bot-adapter-nc)组件全面支持`napcat`

### 2.2.4

* 更新一些依赖版本
* 更新默认的`useragent`
* 优化适配器加载流程
* 优化扫描包

### 2.2.3

* 更新一些依赖版本
* 优化 WebServer 启动流程
* 更新扫描包逻辑支持空格

### 2.2.2

* 更新一些依赖版本
* 修复 WebServer 无法动态注入`route`的问题

### 2.2.1

* 更新一些依赖版本
* 支持多数据源配置
* 优化一些配置

### 2.2.0(不向下兼容)

* 更新一些依赖版本
* 重构`ImageUrl`
* 支持接收`Video`
* 优化`Webdriver`、`Json`、`Scheduling`、`MinIO`
* 延长`longTimeoutWebclient`时间
* 支持`Telegram`(电报机器人)
* 优化启动方式

### 2.1.3

* 优化帮助事件
* `cache`委托新增追加方法

### 2.1.2

* 更新一些依赖版本
* 优化事件
* 优化插件注册器返回内容
* 优化`cache`委托

### 2.1.1

* 发送消息预处理`hook`不再返回默认`message`
* 接受消息添加适配器信息
* 支持`NapCatQQ`所有扩展`api`

### 2.1.0

* 更新一些依赖版本
* 支持`NapCatQQ`
* 所有插件注册器都支持取消注册
* 支持更多消息类型
* 更新默认的`useragent`

### 2.0.0

* 2.0.0-RC?的正式版本
* 修复了一些问题

### 2.0.0-RC1/2/3/4/5/6/7/8(不向下兼容)

* 更新一些依赖版本
* 重做了协议适配器、文本检测、翻译、数据源、MinIO
* `ImageUrl`支持自定义注册
* 修复了一些已知问题
* 支持语音消息
* 支持进退群事件
* 支持撤回消息事件
* 优化了截图分辨率和字体渲染
* 配置文件由`conf`类型改为`yaml`
* 修复`WebClient`可能出现死锁的问题
* 修改`BotApiSupport`注册逻辑

### 1.4.3

* 优化了`ImageUrl`中获取摘要的逻辑
* 更新一些依赖版本

### 1.4.2

* 修复了队列消息无法正常处理顺序的问题
* `ChannelQueue`和`queueMessage`的默认队列大小从0改为无限

### 1.4.1

* 更新一些依赖版本
* 精简了`utils`中内容
* 优化了`ImageUrl`中获取摘要的逻辑
* 使用`github-workflows`发布

### 1.4.0(不向下兼容)

* 修复 message 队列引起的不能重复获取消息问题
* 修复`Webdriver`没有逐级关闭导致残留 chrome 进程的问题
* 新增了在`BotApi.sendGroup`和`BotApi.sendPrivate`方法中指定接受人的参数
* 移除了对`SchedulingBotApi.addReceive`的支持（改为BotApi使用指定接受人的方式发送）

### 1.3.1

* 优化`HookJoinPoint`中`BotApi`的获取逻辑

### 1.3.0(不向下兼容)

* 支持队列消息
* 支持获取引用消息内容
* 发送消息使用链式处理(自定义消息顺序) 注意: 链式消息取代了之前的消息并且不向下兼容
* 将之前的image扩展移动至了expand扩展,并新增了一些扩展(不兼容原路径)
* 更新一些依赖
* 更新默认的`useragent`
* 更新一些描述

### 1.2.6

* 修复(`go-cqhttp`下)群昵称为空字符时无法获取昵称的问题
* 更新一些依赖的版本

### 1.2.5

* 修复任务中使用`SchedulingBotApi.sendAllGroup`会调用未启用的适配器的问题

### 1.2.4

* 修复`帮助功能`的`tag`中没有需要展示的内容时会存在一个空`tag`的问题

### 1.2.3

* 修复多插件-插件内配置文件有不生效问题

### 1.2.2

* 优化启动配置
* 翻译和文本验证判断空字符
* `go-cqhttp`发送失败时重试2次
* 更新一些依赖版本

### 1.2.1

* 插件更新

### 1.2.0

* `hook`增加等待消息
* 新增一些系统内置`hook`

### 1.1.0

* java17 -> java21

### 1.0.1

* 支持`gq-cqhttp`和`qqBot`
