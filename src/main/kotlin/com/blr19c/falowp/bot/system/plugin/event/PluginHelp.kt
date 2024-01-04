package com.blr19c.falowp.bot.system.plugin.event

import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.listener.events.HelpEvent
import com.blr19c.falowp.bot.system.plugin.PluginInfo
import com.blr19c.falowp.bot.system.readResource
import com.blr19c.falowp.bot.system.web.RouteInfo
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
        WebServer.registerRoute(RouteInfo("/plugins") {
            post {
                call.respond(pluginInfoRet)
            }
        })
    }

    override suspend fun invoke(botApi: BotApi, helpEvent: HelpEvent) {
        helpEvent.pluginName ?: return allHelp(botApi, helpEvent)
        return pluginHelp(botApi, helpEvent)
    }

    private suspend fun pluginHelp(botApi: BotApi, helpEvent: HelpEvent) {
        val plugin = pluginList
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
        botApi.sendReply(SendMessage.builder().images(base64Help).build())
    }

    private suspend fun allHelp(botApi: BotApi, helpEvent: HelpEvent) {
        val htmlString = readResource("system/help/help.html") { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        val htmlBody = Jsoup.parse(htmlString)
        val cardList = mutableListOf<String>()
        for (pluginInfo in pluginList) {
            if (pluginInfo.plugin.hidden && !helpEvent.showHidden) continue
            if (!pluginInfo.plugin.enable && !helpEvent.showDisable) continue
            val enablePart = if (pluginInfo.plugin.enable)
                """<div class="status enabled">启用</div>"""
            else
                """<div class="status disabled">停用</div>"""
            val namePart =
                """<div class="card-content"><div class="plugin-info">${pluginInfo.plugin.name}</div></div>"""
            val card = """<div class="card">$enablePart$namePart</div>"""
            cardList.add(card)
        }
        htmlBody.select(".card-container").append(cardList.joinToString(""))
        val base64Help = htmlToImageBase64(htmlBody.html(), ".card-container")
        botApi.sendReply(SendMessage.builder().images(base64Help).build())
    }
}