package com.blr19c.falowp.bot.system.censor

/**
 * 审核结果明细类型
 */
enum class CensorConclusionSubTypeEnum {

    /**
     * 低质灌水
     */
    LOW_QUALITY,

    /**
     * 色情
     */
    PORNOGRAPHY,

    /**
     * 恶意推广
     */
    MALICIOUS_PROMOTION,

    /**
     * 低俗辱骂
     */
    VULGAR_ABUSE,

    /**
     * 隐私信息
     */
    PRIVACY_VIOLATION,

    /**
     * 其他
     */
    OTHER
}