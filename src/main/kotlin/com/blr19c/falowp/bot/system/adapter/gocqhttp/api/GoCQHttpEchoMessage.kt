package com.blr19c.falowp.bot.system.adapter.gocqhttp.api

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 带有echo的回执消息
 */
data class GoCQHttpEchoMessage(

    /**
     * 返回数据
     */
    @field:JsonProperty("data")
    var data: Any? = null,

    /**
     * 回执原请求echo
     */
    @field:JsonProperty("echo")
    var echo: Any? = null,

    /**
     * 额外信息
     */
    @field:JsonProperty("message")
    var message: String? = null,

    /**
     * 状态码
     */
    @field:JsonProperty("retcode")
    var retCode: String? = null,
    /**
     * 状态
     */
    var status: String? = null

)