package com.toshlol.personalfinance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstRunFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `first run creates an account and shows overview`() {
        composeRule.onNodeWithContentDescription("Currency").performTextInput("USD")
        composeRule.onNodeWithContentDescription("Account name").performTextInput("Checking")
        composeRule.onNodeWithText("Create account").performClick()

        composeRule.onNodeWithText("Overview").assertIsDisplayed()
    }
}
