package com.blr19c.falowp.bot.system.api

/**
 * 权限
 */
enum class ApiAuth(val code: Int) {

    /**
     * 超级管理员
     */
    ADMINISTRATOR(Int.MAX_VALUE),

    /**
     * 管理员
     */
    MANAGER(100),

    /**
     * 普通成员
     */
    ORDINARY_MEMBER(0),
}