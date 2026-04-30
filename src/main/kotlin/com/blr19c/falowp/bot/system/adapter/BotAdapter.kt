package com.blr19c.falowp.bot.system.adapter

/**
 * 协议适配器
 *
 * @param name 协议名称
 */
annotation class BotAdapter(val name: String)

/**
 * 协议适配器
 */
interface BotAdapterInterface {

    /**
     * 启动协议适配器
     */
    suspend fun start(register: BotAdapterRegister)

}

/**
 * 协议适配器注册
 */
@Suppress("UNUSED")
class BotAdapterRegister(private val registerList: MutableList<BotAdapterInterface>) {

    /**
     * 完成协议适配器注册
     */
    fun finish(botAdapterInterface: BotAdapterInterface) {
        registerList.add(botAdapterInterface)
    }
}