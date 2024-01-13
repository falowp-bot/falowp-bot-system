package com.blr19c.falowp.bot.system.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.image.encodeToBase64String
import com.microsoft.playwright.*
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.ViewportSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Webdriver截图使用
 */
object Webdriver : Log {

    internal fun configure() {
        log().info("初始化Webdriver")
        Playwright.create().close()
        log().info("初始化Webdriver完成")
    }
}

/**
 * 存在这个元素的情况下执行
 */
fun <T> Page.existsToExecute(
    selector: String,
    onlyOne: Boolean = true,
    block: ElementHandle.() -> T
): List<T> {
    val allElementHandle = this.querySelectorAll(selector)
    if (allElementHandle.isNullOrEmpty()) return emptyList()
    if (onlyOne) return listOf(block.invoke(allElementHandle.first()))
    return allElementHandle.map { block.invoke(it) }.toList()
}

fun defaultBrowserContext(): BrowserContext {
    return Playwright.create().chromium().launch().newContext(
        Browser.NewContextOptions()
            .setViewportSize(ViewportSize(1920, 1080))
            .setUserAgent(commonUserAgent())
            .setIsMobile(false)
    )
}

fun <T> commonWebdriverContext(block: BrowserContext.() -> T): T {
    return defaultBrowserContext().use { block.invoke(it) }
}

suspend fun htmlToImageBase64(html: String, querySelector: String = "body"): String {
    return withContext(Dispatchers.IO) {
        commonWebdriverContext {
            this.newPage().use { page ->
                page.setContent(html)
                page.waitForLoadState(LoadState.NETWORKIDLE)
                page.querySelector(querySelector).screenshot().encodeToBase64String()
            }
        }
    }
}