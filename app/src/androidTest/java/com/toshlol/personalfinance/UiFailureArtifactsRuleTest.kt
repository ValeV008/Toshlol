package com.toshlol.personalfinance

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UiFailureArtifactsRuleTest {
    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun captureVisibleFailureBeforeCleanupAndPreserveOriginalException() {
        composeRule.setContent { Text("Failure capture probe") }
        val context: Context = ApplicationProvider.getApplicationContext()
        val outputRoot: File = File(context.cacheDir, "failure-capture-${UUID.randomUUID()}")
        val failure: AssertionError = AssertionError("Deliberate failure capture probe")
        val description: Description = Description.createTestDescription(javaClass, "captureProbe")
        val statement: Statement =
            object : Statement() {
                override fun evaluate(): Unit = throw failure
            }
        try {
            try {
                UiFailureArtifactsRule(composeRule, outputRoot).apply(statement, description).evaluate()
                fail("Expected the original assertion")
            } catch (actualFailure: AssertionError) {
                assertSame(failure, actualFailure)
            }
            val directory: File = outputRoot.walkTopDown().first { it.name == "failure.txt" }.parentFile!!
            assertTrue(File(directory, "failure.txt").readText().contains("Deliberate failure capture probe"))
            assertTrue(File(directory, "semantics.txt").readText().contains("Failure capture probe"))
            assertTrue(File(directory, "logcat.txt").length() > 0L)
            assertNotNull(BitmapFactory.decodeFile(File(directory, "screenshot.png").absolutePath)?.also { it.recycle() })
            assertEquals(emptyList<Throwable>(), failure.suppressed.toList())
        } finally {
            outputRoot.deleteRecursively()
        }
    }
}
