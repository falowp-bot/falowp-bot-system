package com.blr19c.falowp.bot.system.translate

import com.blr19c.falowp.bot.system.systemConfigProperty
import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.tmt.v20180321.TmtClient
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 腾讯翻译
 */
object TxTranslate {

    private val client by lazy {
        TmtClient(
            Credential(
                systemConfigProperty("translate.tx.secretId"),
                systemConfigProperty("translate.tx.secretKey")

            ),
            systemConfigProperty("translate.tx.region")
        )
    }

    /**
     * 中文转英文
     */
    suspend fun cnToEn(query: String): String {
        if (query.isBlank()) {
            return query
        }
        val chineseRegex = Regex("[\\u4E00-\\u9FA5]+")
        if (!chineseRegex.containsMatchIn(query)) {
            return query
        }
        val request = TextTranslateRequest()
        request.sourceText = query
        request.source = "zh"
        request.target = "en"
        request.projectId = 0L
        val response = withContext(Dispatchers.IO) {
            client.TextTranslate(request)
        }
        return response.targetText
    }
}