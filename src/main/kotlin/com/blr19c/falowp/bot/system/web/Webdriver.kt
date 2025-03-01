package com.blr19c.falowp.bot.system.web

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.expand.encodeToBase64String
import com.microsoft.playwright.*
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.options.LoadState
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

fun defaultNewContextOptions(): Browser.NewContextOptions {
    return Browser.NewContextOptions()
        .setViewportSize(3024, 1964)
        .setDeviceScaleFactor(3.0)
        .setUserAgent(commonUserAgent())
        .setIsMobile(false)
}

fun defaultLaunchOptions(): LaunchOptions {
    return LaunchOptions().setArgs(
        listOf(
            "--font-render-hinting=medium",
            "--font-render-hinting=none",
            "--disable-font-subpixel-positioning",
            "--enable-font-antialiasing",
            "--enable-harfbuzz-rendertext"
        )
    )
}

fun <T> commonWebdriverContext(browserContext: BrowserContext? = null, block: BrowserContext.() -> T): T {
    if (browserContext != null) {
        return browserContext.use { block(it) }
    }
    return Playwright.create().use { playwright ->
        playwright.chromium().launch(defaultLaunchOptions()).use { browser ->
            browser.newContext(defaultNewContextOptions()).use { browserContext ->
                block.invoke(browserContext)
            }
        }
    }
}

fun <T> commonWebdriverContextPage(page: Page? = null, block: Page.() -> T): T {
    if (page != null) {
        return page.use { block(it) }
    }
    return commonWebdriverContext { this.newPage().use { page -> block(page) } }
}


suspend fun htmlToImageBase64(html: String, querySelector: String = "body"): String {
    return withContext(Dispatchers.IO) {
        commonWebdriverContextPage {
            this.setContent(html)
            this.waitForLoadState(LoadState.NETWORKIDLE)
            this.querySelector(querySelector).screenshot().encodeToBase64String()
        }
    }
}