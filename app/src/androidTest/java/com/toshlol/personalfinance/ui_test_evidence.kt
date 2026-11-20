@file:Suppress("ktlint:standard:filename")

package com.toshlol.personalfinance

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.OutputStream

internal data class TestScreenshotEvidence(
    val directoryName: String,
    val fileName: String,
    val screenshot: Bitmap? = null,
)

@Suppress("ktlint:standard:no-unit-return")
internal fun saveTestScreenshot(evidence: TestScreenshotEvidence): Unit {
    val context: Context = ApplicationProvider.getApplicationContext()
    val outputRoot: File =
        InstrumentationRegistry
            .getArguments()
            .getString("additionalTestOutputDir")
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
            ?: context.cacheDir
    val outputDirectory: File = File(outputRoot, evidence.directoryName)
    check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
    val screenshot: Bitmap =
        evidence.screenshot ?: checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    try {
        File(outputDirectory, evidence.fileName).outputStream().use { output: OutputStream ->
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    } finally {
        screenshot.recycle()
    }
}
