package com.blr19c.falowp.bot.system.censor

/**
 * 审核结果
 */
data class CensorResult(
    /**
     * 审核结果
     */
    val conclusionType: CensorConclusionTypeEnum,
    /**
     * 审核结果数据
     */
    val items: List<CensorResultItem> = emptyList()
)

/**
 * 审核结果数据
 */
data class CensorResultItem(
    /**
     * 审核结果
     */
    val conclusionType: CensorConclusionTypeEnum,
    /**
     * 审核结果明细类型
     */
    val conclusionSubType: CensorConclusionSubTypeEnum,
    /**
     * 命中数据
     */
    val hits: List<CensorResultItemHit> = emptyList()
)

/**
 * 审核结果命中数据
 */
data class CensorResultItemHit(
    /**
     * 内容
     */
    val words: List<String>,
    /**
     * 置信度0-1
     */
    val probability: Double? = null
)