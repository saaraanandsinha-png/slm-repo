package com.example.saaraapp

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Downloads the FunctionGemma 270M (int8) model file on first launch
 * and stores it in the app's private files directory.
 *
 * The model is only downloaded once — subsequent launches reuse the cached file.
 */
object ModelDownloadManager {

    private const val TAG = "ModelDownload"

    /**
     * Filename used to store the model locally.
     * Updated to match the actual file in assets.
     */
    private const val MODEL_FILE_NAME = "functiongemma-270m-it-Q4_K_M.gguf"

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns the local model [File] if it already exists, otherwise null. */
    fun getModelFileIfExists(context: Context): File? {
        val file = modelFile(context)
        return if (file.exists() && file.length() > 0L) file else null
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun modelFile(context: Context): File =
        File(context.filesDir, MODEL_FILE_NAME)
}
