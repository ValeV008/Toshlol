package com.toshlol.personalfinance

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

/** Waits for observable UI state, including controls created by lazy containers. */
class FinanceUiDriver(
    private val rule: ComposeTestRule,
) {
    fun showOverview() {
        rule.waitUntil(TIMEOUT_MILLIS) { rule.onAllNodesWithTag("overview-root").fetchSemanticsNodes().size == 1 }
        showText("CLOSING BALANCE")
    }

    fun selectOverviewAccount(name: String) {
        showOverview()
        rule.onNodeWithTag("overview-root").performTouchInput {
            swipe(start = center.copy(x = center.x * 2f - 1f), end = center.copy(x = center.x * 0.8f), durationMillis = 450)
        }
        rule
            .onNodeWithContentDescription("$name Account selection")
            .assertIsOff()
            .performClick()
            .assertIsOn()
        clickText("Close")
    }

    fun showText(
        text: String,
        containerTag: String? = null,
        substring: Boolean = false,
    ): SemanticsNodeInteraction = show(hasText(text, substring = substring), containerTag)

    fun clickText(
        text: String,
        containerTag: String? = null,
    ): Unit = click(hasText(text), containerTag)

    fun click(
        matcher: SemanticsMatcher,
        containerTag: String? = null,
    ) {
        val node: SemanticsNodeInteraction = show(matcher, containerTag)
        rule.waitUntil(TIMEOUT_MILLIS) { rule.onAllNodes(matcher and isEnabled()).fetchSemanticsNodes().size == 1 }
        node.assertHasClickAction().performClick()
    }

    fun enterText(
        label: String,
        value: String,
        containerTag: String? = null,
    ) {
        if (label == "Amount" && (containerTag == "transaction-form" || containerTag == "transfer-form")) {
            rule.onNodeWithTag("amount-key-C").performClick()
            value.filter(Char::isDigit).forEach { digit ->
                rule.onNodeWithTag("amount-key-$digit").performClick()
            }
            return
        }
        showText(label, containerTag).performTextReplacement(value)
        dismissKeyboard()
    }

    fun show(
        matcher: SemanticsMatcher,
        containerTag: String? = null,
    ): SemanticsNodeInteraction {
        dismissKeyboard()
        if (containerTag != null) showInContainer(matcher, containerTag)
        rule.waitUntil(TIMEOUT_MILLIS) { rule.onAllNodes(matcher).fetchSemanticsNodes().size == 1 }
        val node: SemanticsNodeInteraction = rule.onNode(matcher)
        if (!node.isDisplayed()) node.performScrollTo()
        rule.waitUntil(TIMEOUT_MILLIS) { node.isDisplayed() }
        return node.assertIsDisplayed()
    }

    fun dismissKeyboard() {
        rule.runOnUiThread {
            WindowInspector.getGlobalWindowViews().forEach { view: View ->
                view.context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (device.findObject(By.pkg("com.google.android.inputmethod.latin")) != null) device.pressBack()
        rule.waitUntil(TIMEOUT_MILLIS) {
            device.findObject(By.pkg("com.google.android.inputmethod.latin")) == null
        }
    }

    private fun showInContainer(
        matcher: SemanticsMatcher,
        containerTag: String,
    ) {
        rule.waitUntil(TIMEOUT_MILLIS) {
            try {
                rule.onNodeWithTag(containerTag).performScrollToNode(matcher)
                true
            } catch (notYetPresent: AssertionError) {
                false
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS: Long = 15_000L
    }
}
