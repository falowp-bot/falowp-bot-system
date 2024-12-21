package com.blr19c.falowp.bot.system.plugin.event

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.api.SourceTypeEnum.*
import com.blr19c.falowp.bot.system.listener.events.HelpEvent
import com.blr19c.falowp.bot.system.listener.hooks.HelpEventHook
import com.blr19c.falowp.bot.system.plugin.PluginInfo
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook
import com.blr19c.falowp.bot.system.readResource
import com.blr19c.falowp.bot.system.web.WebServer
import com.blr19c.falowp.bot.system.web.htmlToImageBase64
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jsoup.Jsoup

class PluginHelp(private val pluginList: List<PluginInfo>) : suspend (BotApi, HelpEvent) -> Unit {
    private val pluginInfoRet = pluginList.map { it.plugin }.filter { !it.hidden }.map {
        mapOf(
            "name" to it.name,
            "desc" to it.desc,
            "enable" to it.enable,
        )
    }

    init {
        pluginList.sortedBy { it.plugin.name }
        WebServer.registerRoute {
            post("/plugins") {
                call.respond(pluginInfoRet)
            }
        }
    }

    private suspend fun withPlugin(botApi: BotApi): List<PluginInfo> {
        val helpEventHook = HelpEventHook(pluginList)
        withPluginHook(botApi, helpEventHook) {}
        return helpEventHook.pluginInfo
    }


    override suspend fun invoke(botApi: BotApi, helpEvent: HelpEvent) {
        helpEvent.pluginName ?: return allHelp(botApi, helpEvent)
        return pluginHelp(botApi, helpEvent)
    }

    private suspend fun pluginHelp(botApi: BotApi, helpEvent: HelpEvent) {
        val plugin = withPlugin(botApi)
            .filter { it.plugin.name == helpEvent.pluginName }
            .filter { it.plugin.enable || helpEvent.showDisable }
            .find { !it.plugin.hidden || helpEvent.showHidden }
            ?.plugin
            ?: return botApi.sendReply("没有此功能的帮助")
        val htmlString = readResource("system/help/pluginHelp.html") { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        val htmlBody = Jsoup.parse(htmlString)
        htmlBody.select("#pluginName").html(plugin.name)
        htmlBody.select("#pluginDesc").html(plugin.desc)
        val base64Help = htmlToImageBase64(htmlBody.html(), ".card-container")
        botApi.send(helpEvent, SendMessage.builder().image(base64Help).build())
    }

    private suspend fun allHelp(botApi: BotApi, helpEvent: HelpEvent) {
        val htmlString = readResource("system/help/help.html") { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        val htmlBody = Jsoup.parse(htmlString)
        val tagList = mutableListOf<String>()
        for ((tag, pluginInfoList) in withPlugin(botApi).groupBy { it.plugin.tag }) {
            val cardList = mutableListOf<String>()
            for (pluginInfo in pluginInfoList) {
                if (pluginInfo.plugin.hidden && !helpEvent.showHidden) continue
                if (!pluginInfo.plugin.enable && !helpEvent.showDisable) continue
                val enablePart = if (pluginInfo.plugin.enable) "" else " disable"
                val plugin = """<div class="plugin-info$enablePart">${pluginInfo.plugin.name}</div>"""
                val content = """<div class="card-content">$plugin</div>"""
                val card = """<div class="card-item">$content</div>"""
                cardList.add(card)
            }
            if (cardList.isNotEmpty()) {
                tagList.add("""<div class="card"><div class="card-tag">$tag</div>${cardList.joinToString("")}</div>""")
            }
        }
        val card = tagList.sortedByDescending { it.length }.joinToString("")
        htmlBody.select(".card-container").append(card)
        val base64Help = htmlToImageBase64(htmlBody.html(), ".card-container")
        botApi.send(helpEvent, SendMessage.builder().image(base64Help).build())
    }

    private suspend fun BotApi.send(event: HelpEvent, message: SendMessageChain) {
        when (event.sourceType) {
            GROUP -> this.sendGroup(message, sourceId = event.sourceId!!)
            PRIVATE -> this.sendPrivate(message, sourceId = event.sourceId!!)
            UNKNOWN, null -> this.sendReply(message)
        }
    }
}