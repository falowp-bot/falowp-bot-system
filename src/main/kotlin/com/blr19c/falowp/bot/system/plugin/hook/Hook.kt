package com.blr19c.falowp.bot.system.plugin.hook

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.listener.hooks.SendMessageHook
import com.blr19c.falowp.bot.system.plugin.*
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook.Companion.beforeHook
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 钩子函数
 * @param hook 钩子的信息
 * @param botApi 当前位置的botApi
 * @param block 执行内容
 */
suspend fun withPluginHook(
    botApi: BotApi,
    hook: Plugin.Listener.Hook,
    block: suspend () -> Unit
) {
    HookManager.buildHookJoinPoint(hook, botApi, block).process()
}

/**
 * 注册运行时钩子
 * 注意:请使用hook内的botApi而不是注册hook的botApi
 */
inline fun <reified T : Plugin.Listener.Hook> BotApi.runtimeHook(
    hookType: HookTypeEnum,
    order: Int = 0,
    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
    noinline block: suspend HookJoinPoint.(T, UnRegister) -> Unit
): UnRegister {
    var hook: UnRegister? = null
    hook = HookPluginRegister(order, T::class, hookType, match, {
        block.invoke(this, it, hook!!)
    })
    hook.register()
    return hook
}

/**
 * 监听下次匹配的消息
 */
suspend fun <T : Any> BotApi.awaitReply(
    match: MessagePluginRegisterMatch = MessagePluginRegisterMatch.allMatch(),
    block: suspend BotApi.(args: Array<String>) -> T
): T {
    var data: T? = null
    this.runtimeHook<ReceiveMessageHook>(HookTypeEnum.BEFORE) { (receiveMessage), unRegister ->
        val botApi = this.botApi()
        if (match.checkMath(receiveMessage)) {
            val args = match.regex?.find(receiveMessage.content.message)?.destructured?.toList() ?: listOf()
            try {
                data = block.invoke(botApi, args.toTypedArray())
            } finally {
                unRegister.unregister()
            }
            return@runtimeHook
        }
    }
    while (isActive && data == null) {
        delay(100)
    }
    return data!!
}


/**
 * 发送消息预处理hook
 */
fun sendMessageHook(block: suspend HookJoinPoint.(List<SendMessage>) -> List<SendMessage>): Register {
    return beforeHook<SendMessageHook> { sendMessageHook ->
        val originalMessageList = sendMessageHook.sendMessageChain.toList()
        sendMessageHook.sendMessageChain.clear()
        for (sendMessageChain in originalMessageList) {
            val messageList = sendMessageChain.messageList
            val newMessageList = block(messageList)
            val newSendMessageChain = sendMessageChain.copy(id = sendMessageChain.id, messageList = newMessageList)
            sendMessageHook.sendMessageChain.add(newSendMessageChain)
        }
        this.process()
    }
}
