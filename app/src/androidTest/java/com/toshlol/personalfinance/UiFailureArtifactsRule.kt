package com.toshlol.personalfinance

import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.printToString
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

/** Place inside the Compose rule so capture happens before the activity is destroyed. */
class UiFailureArtifactsRule(
    private val rule: ComposeTestRule,
    private val outputRoot: File = findOutputRoot(),
) : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (failure: Throwable) {
                    runCatching { captureFailure(description, failure) }.onFailure(failure::addSuppressed)
                    throw failure
                }
            }
        }

    private fun captureFailure(
        description: Description,
        failure: Throwable,
    ) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory: File =
            File(outputRoot, "ui-failures/${description.className}.${description.methodName}/${System.currentTimeMillis()}")
        check(directory.mkdirs()) { "Could not create failure output: $directory" }
        File(directory, "failure.txt").writeText(failure.stackTraceToString())
        captureArtifact(directory, "screenshot.png", failure) { file: File ->
            val screenshot: Bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Screenshot unavailable" }
            try {
                file.outputStream().use { check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                screenshot.recycle()
            }
        }
        captureArtifact(directory, "logcat.txt", failure) { file: File ->
            ParcelFileDescriptor
                .AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("logcat -d -t 2000 -v threadtime"))
                .use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        }
        captureArtifact(directory, "semantics.txt", failure) { file: File ->
            val roots: Int = rule.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().size
            check(roots > 0) { "No Compose roots available" }
            file.writeText(
                (0 until roots).joinToString("\n\n") { index: Int ->
                    "Root $index\n${rule.onAllNodes(isRoot(), useUnmergedTree = true)[index].printToString()}"
                },
            )
        }
        Log.e("UiFailureArtifacts", "Failure artifacts: ${directory.absolutePath}")
    }

    private fun captureArtifact(
        directory: File,
        name: String,
        failure: Throwable,
        capture: (File) -> Unit,
    ) {
        runCatching { capture(File(directory, name)) }.onFailure { captureFailure: Throwable ->
            failure.addSuppressed(captureFailure)
            File(directory, "$name.error.txt").writeText(captureFailure.stackTraceToString())
        }
    }

    private companion object {
        fun findOutputRoot(): File =
            InstrumentationRegistry
                .getArguments()
                .getString("additionalTestOutputDir")
                ?.takeIf(String::isNotBlank)
                ?.let { File(it) }
                ?: File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "test-output")
    }
}
