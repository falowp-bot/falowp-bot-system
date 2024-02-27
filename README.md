![maven](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)
![maven](https://img.shields.io/badge/Ktor-3.0.0-a.svg)
![maven](https://img.shields.io/badge/go--cqhttp-1.2.0-red)
![maven](https://img.shields.io/badge/qq-bot-red)

# 小花落Bot

> 此项目基于qqBot和go-cqhttp开发的机器人框架

## 当前最新版本

```
// https://mvnrepository.com/artifact/com.blr19c.falowp/falowp-bot-system
implementation("com.blr19c.falowp:falowp-bot-system:1.3.1")
```

## [查看文档](https://falowp.blr19c.com)

## 更新日志

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