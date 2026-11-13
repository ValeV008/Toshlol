package com.toshlol.personalfinance

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Waits for observable UI state, including controls created by lazy containers. */
class FinanceUiDriver(
    private val rule: ComposeTestRule,
) {
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
        rule.waitUntil(TIMEOUT_MILLIS) {
            rule.runOnUiThread {
                WindowInspector.getGlobalWindowViews().none { view: View ->
                    ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                }
            }
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
