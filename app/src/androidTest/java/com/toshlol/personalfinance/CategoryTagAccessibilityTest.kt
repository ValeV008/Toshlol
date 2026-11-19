package com.toshlol.personalfinance

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.AccountEditorActions
import com.toshlol.personalfinance.ui.CategoryActions
import com.toshlol.personalfinance.ui.CategoryTagScreen
import com.toshlol.personalfinance.ui.OrganizerActions
import com.toshlol.personalfinance.ui.TagActions
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class CategoryTagAccessibilityTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    fun operateClassificationManagementAtLargeFont() {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    CategoryTagScreen(
                        state = createLargeFontState(),
                        actions = createNoOpOrganizerActions(),
                    )
                }
            }
        }
        composeRule.onNodeWithText("Categories & Tags").assertIsDisplayed()
        composeRule.onNodeWithText("Category name").assertIsDisplayed()
        composeRule
            .onNodeWithTag("category-direction-EXPENSE")
            .performScrollTo()
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("category-direction-INCOME").performScrollTo().assertHasClickAction()
        composeRule.onNodeWithText("Add category").performScrollTo().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Choose Category icon").assertDoesNotExist()
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Past medical · expense · Archived"))
        composeRule.onNodeWithTag("category-row-retired").assertIsDisplayed()
        composeRule.onNodeWithTag("edit-category-retired").assertHasClickAction()
        composeRule.onNodeWithTag("archive-category-retired").assertDoesNotExist()
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Old label"))
        composeRule.onNodeWithTag("tag-association-independent").performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Old label"))
        composeRule.onNodeWithTag("tag-row-old-tag").assertIsDisplayed()
        composeRule.onNodeWithTag("edit-tag-old-tag").assertHasClickAction()
        composeRule.onNodeWithTag("archive-tag-old-tag").assertDoesNotExist()
        saveScreenEvidence("large-font-management.png")
    }

    private fun createLargeFontState(): TransactionsUiState =
        TransactionsUiState(
            categoryDirection = TransactionDirection.EXPENSE,
            categories =
                listOf(
                    FinanceCategory("food", "Food", TransactionDirection.EXPENSE, false),
                    FinanceCategory("retired", "Past medical", TransactionDirection.EXPENSE, true),
                    FinanceCategory("salary", "Salary", TransactionDirection.INCOME, false),
                ),
            tags =
                listOf(
                    FinanceTag("lunch", "Lunch", "food", "Food", false),
                    FinanceTag("old-tag", "Old label", null, null, true),
                ),
        )

    private fun createNoOpOrganizerActions(): OrganizerActions =
        OrganizerActions(
            open = {},
            close = {},
            categories =
                CategoryActions(
                    nameChanged = {},
                    directionChanged = {},
                    save = {},
                    edit = {},
                    archive = {},
                    cancelEditing = {},
                ),
            tags =
                TagActions(
                    nameChanged = {},
                    categoryChanged = {},
                    save = {},
                    edit = {},
                    archive = {},
                    cancelEditing = {},
                ),
            accounts =
                AccountEditorActions(
                    nameChanged = {},
                    select = {},
                    openEditor = {},
                    beginCreate = {},
                    closeEditor = {},
                    save = {},
                    openingBalanceChanged = {},
                    openingDateChanged = {},
                    newNameChanged = {},
                    newTypeChanged = {},
                    newOpeningBalanceChanged = {},
                    newOpeningDateChanged = {},
                    create = {},
                    archive = {},
                ),
        )

    private fun saveScreenEvidence(fileName: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputRoot: File =
            InstrumentationRegistry
                .getArguments()
                .getString("additionalTestOutputDir")
                ?.takeIf(String::isNotBlank)
                ?.let(::File)
                ?: context.cacheDir
        val outputDirectory: File = File(outputRoot, "classification-evidence")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot: Bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(outputDirectory, fileName).outputStream().use { output: OutputStream ->
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            screenshot.recycle()
        }
    }
}
