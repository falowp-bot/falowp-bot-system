package com.blr19c.falowp.bot.system.censor

/**
 * 审核结果类型
 */
enum class CensorConclusionTypeEnum {

    /**
     * 合规
     */
    COMPLIANT,

    /**
     * 不合规
     */
    NON_COMPLIANT,

    /**
     * 疑似
     */
    SUSPICIOUS,

    /**
     * 审核失败
     */
    AUDIT_FAILURE,
}