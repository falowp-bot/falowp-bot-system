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

    suspend fun start(register: BotAdapterRegister)

}

/**
 * 协议适配器注册
 */
class BotAdapterRegister(private val registerList: MutableList<BotAdapterInterface>) {

    fun finish(botAdapterInterface: BotAdapterInterface) {
        registerList.add(botAdapterInterface)
    }
}