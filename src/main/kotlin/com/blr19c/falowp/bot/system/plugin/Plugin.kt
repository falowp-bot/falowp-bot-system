package com.blr19c.falowp.bot.system.plugin

import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.MessageSubTypeEnum
import com.blr19c.falowp.bot.system.plugin.hook.HookJoinPoint
import com.blr19c.falowp.bot.system.plugin.hook.HookTypeEnum
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApi
import com.blr19c.falowp.bot.system.scheduling.cron.ApplicationInitTrigger
import com.blr19c.falowp.bot.system.scheduling.cron.CronTrigger
import com.blr19c.falowp.bot.system.scheduling.cron.PeriodicTrigger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

annotation class Plugin(
    /**
     * 插件名称
     */
    val name: String,
    /**
     * 插件描述(支持html)
     */
    val desc: String = "",
    /**
     * 标签(相同标签的插件在默认的帮助展示时会放在一起)
     */
    val tag: String = "其他",
    /**
     * 是否启用
     */
    val enable: Boolean = true,
    /**
     * 是否隐藏
     */
    val hidden: Boolean = false,
) {

    /**
     * 监听器
     */
    interface Listener {

        /**
         * 事件
         */
        interface Event {
            companion object {
                /**
                 * 订阅事件(内部事件注册)
                 */
                inline fun <reified T : Event> eventListener(noinline block: suspend BotApi.(T) -> Unit): Register {
                    return EventPluginRegister(T::class, block)
                }
            }
        }

        /**
         * 在执行期间注入
         */
        interface Hook {
            companion object {

                /**
                 * 钩子
                 * @param hookType 类型
                 * @param order 顺序
                 * @param block 执行内容
                 */
                inline fun <reified T : Hook> hook(
                    hookType: HookTypeEnum,
                    order: Int = 0,
                    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
                    noinline block: suspend HookJoinPoint.(T) -> Unit
                ): Register {
                    return HookPluginRegister(order, T::class, hookType, match, block)
                }

                /**
                 * 前置
                 */
                inline fun <reified T : Hook> beforeHook(
                    order: Int = 0,
                    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
                    noinline block: suspend HookJoinPoint.(T) -> Unit
                ): Register {
                    return hook<T>(HookTypeEnum.BEFORE, order, match, block)
                }

                /**
                 * 后置
                 */
                inline fun <reified T : Hook> afterReturningHook(
                    order: Int = 0,
                    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
                    noinline block: suspend HookJoinPoint.(T) -> Unit
                ): Register {
                    return hook<T>(HookTypeEnum.AFTER_RETURNING, order, match, block)
                }

                /**
                 * 异常通知
                 */
                inline fun <reified T : Hook> afterThrowingHook(
                    order: Int = 0,
                    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
                    noinline block: suspend HookJoinPoint.(T) -> Unit
                ): Register {
                    return hook<T>(HookTypeEnum.AFTER_THROWING, order, match, block)
                }

                /**
                 * 最终通知
                 */
                inline fun <reified T : Hook> afterFinallyHook(
                    order: Int = 0,
                    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
                    noinline block: suspend HookJoinPoint.(T) -> Unit
                ): Register {
                    return hook<T>(HookTypeEnum.AFTER_FINALLY, order, match, block)
                }

                /**
                 * 环绕
                 */
                inline fun <reified T : Hook> aroundHook(
                    order: Int = 0,
                    match: HookPluginRegisterMatch = HookPluginRegisterMatch.allMatch(),
                    noinline block: suspend HookJoinPoint.(T) -> Unit
                ): Register {
                    return hook<T>(HookTypeEnum.AROUND, order, match, block)
                }
            }
        }
    }

    object Task {

        /**
         * cron表达式执行任务
         * @param cron cron表达式
         * @param block 执行内容
         */
        fun cronScheduling(cron: String, block: suspend SchedulingBotApi.() -> Unit): Register {
            return TaskPluginRegister(CronTrigger(cron), block)
        }

        /**
         * 周期执行任务
         *
         * @param period 执行周期
         * @param initialDelay 首次执行延时时间
         * @param fixedRate 是否为固定速率执行
         * @param block 执行内容
         */
        fun periodicScheduling(
            period: Duration,
            initialDelay: Duration = 0.seconds,
            fixedRate: Boolean = false,
            block: suspend SchedulingBotApi.() -> Unit
        ): Register {
            return TaskPluginRegister(PeriodicTrigger(fixedRate, period, initialDelay), block)
        }

        /**
         * 在程序完全启动之后执行
         *
         * @param block 执行内容
         */
        fun applicationInitScheduling(block: suspend SchedulingBotApi.() -> Unit): Register {
            return TaskPluginRegister(ApplicationInitTrigger(), block)
        }

    }

    object Message {

        /**
         * 消息
         *
         * @param regex 正则匹配
         * @param terminateEvent 执行完终止事件传播
         * @param block 执行内容
         */
        fun message(
            regex: Regex,
            order: Int = 0,
            auth: ApiAuth = ApiAuth.ORDINARY_MEMBER,
            terminateEvent: Boolean = true,
            block: suspend BotApi.(args: Array<String>) -> Unit
        ): Register {
            return MessagePluginRegister(
                order,
                MessagePluginRegisterMatch(regex, auth),
                terminateEvent,
                block
            )
        }


        /**
         * 戳一戳
         *
         * @param terminateEvent 执行完终止事件传播
         * @param block 执行内容
         */
        fun poke(
            order: Int = 0,
            auth: ApiAuth = ApiAuth.ORDINARY_MEMBER,
            terminateEvent: Boolean = true,
            block: suspend BotApi.(args: Array<String>) -> Unit
        ): Register {
            return MessagePluginRegister(
                order,
                MessagePluginRegisterMatch(messageSubType = MessageSubTypeEnum.POKE, auth = auth, atMe = true),
                terminateEvent,
                block
            )
        }

        /**
         * 消息
         *
         * @param match 匹配规则
         * @param terminateEvent 执行完终止事件传播
         * @param block 执行内容
         */
        fun message(
            match: MessagePluginRegisterMatch = MessagePluginRegisterMatch.allMatch(),
            order: Int = 0,
            terminateEvent: Boolean = true,
            block: suspend BotApi.(args: Array<String>) -> Unit
        ): Register {
            return MessagePluginRegister(
                order,
                match,
                terminateEvent,
                block
            )
        }
    }
}