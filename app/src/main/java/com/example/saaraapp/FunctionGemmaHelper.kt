package com.example.saaraapp

import android.content.Context
import android.util.Log
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * Wraps Llama.cpp (via LLamatik) to run FunctionGemma 270M on-device.
 *
 * ## How it works
 * 1. **Initialize** — [initialize] loads the GGUF model file into memory via [LlamaBridge].
 *    If the file isn't on disk yet, it tries to copy it from the app's assets folder first.
 * 2. **Prompt** — [buildPrompt] wraps the incoming WhatsApp message in a Gemma chat template,
 *    instructing the model to return a structured JSON reminder object.
 * 3. **Generate** — [generateInternal] runs streaming inference via [LlamaBridge.generateStream].
 *    Streaming is used instead of a blocking call for better reliability and so we can
 *    manually intercept Gemma's stop sequences (e.g. `<end_of_turn>`).
 * 4. **Parse** — [parseResponse] extracts and validates the JSON from the raw model output.
 * 5. **Fallback** — If the model isn't ready or JSON parsing fails, [fallback] kicks in
 *    and uses rule-based [KeywordExtractor] logic instead.
 */
class FunctionGemmaHelper(private val context: Context) {

    // ── Initialise ────────────────────────────────────────────────────────────

    /**
     * Loads the model. If [modelFile] does not exist, it tries to copy it
     * from assets to the internal files directory first.
     */
    suspend fun initialize(modelFile: File) = withContext(Dispatchers.IO) {
        try {
            if (isReady) return@withContext

            var finalFile = modelFile
            
            // If the provided file doesn't exist, try to copy from assets
            if (!finalFile.exists()) {
                val assetName = modelFile.name
                val internalFile = File(context.filesDir, assetName)
                
                if (internalFile.exists() && internalFile.length() > 0) {
                    finalFile = internalFile
                } else {
                    try {
                        context.assets.open(assetName).use { input ->
                            internalFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        finalFile = internalFile
                        Log.i(TAG, "Copied $assetName from assets to internal storage")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not find/copy $assetName from assets: ${e.message}")
                    }
                }
            }

            if (!finalFile.exists()) {
                Log.e(TAG, "Model file not found at ${finalFile.absolutePath}")
                return@withContext
            }

            // Set parameters BEFORE initialization
            LlamaBridge.updateGenerateParams(
                temperature = 0.1f,
                maxTokens = 128,
                topP = 0.95f,
                topK = 40,
                repeatPenalty = 1.1f,
                contextLength = 1024,
                numThreads = 4,
                useMmap = true,
                flashAttention = false,
                batchSize = 512,
                gpuLayers = 0
            )

            val ok = LlamaBridge.initGenerateModel(finalFile.absolutePath)
            if (ok) {
                isReady = true
                Log.i(TAG, "FunctionGemma loaded and tuned from ${finalFile.name}")
            } else {
                Log.e(TAG, "LlamaBridge failed to load model from ${finalFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "LlamaBridge error: ${e.message}")
            isReady = false
        }
    }

    // ── Analyze ───────────────────────────────────────────────────────────────

    /**
     * Analyzes [message] and returns a [GemmaResult].
     */
    suspend fun analyze(message: String): GemmaResult = withContext(Dispatchers.Default) {
        if (!isReady) {
            Log.d(TAG, "Model not ready — using KeywordExtractor fallback")
            return@withContext fallback(message)
        }

        return@withContext try {
            val prompt = buildPrompt(message)
            Log.d(TAG, "Starting generation...")
            
            val rawResponse = generateInternal(prompt)
            Log.d(TAG, "Generation finished. Raw length: ${rawResponse.length}")

            parseResponse(rawResponse) ?: fallback(message)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message} — using fallback")
            fallback(message)
        }
    }

    /**
     * General chat method that returns the raw string from the model.
     * Used for testing the model's raw performance in the Chat tab.
     */
    suspend fun askGemma(message: String): String = withContext(Dispatchers.Default) {
        if (!isReady) return@withContext "Model not ready."

        try {
            val messages = listOf("user" to message)
            val prompt = LlamaBridge.applyChatTemplate(messages, true) ?: message
            Log.d(TAG, "Chatting with prompt: $prompt")
            generateInternal(prompt)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Internal helper that wraps streaming generation into a suspend function.
     *
     * We use [suspendCancellableCoroutine] to bridge the callback-based [LlamaBridge.generateStream]
     * API into Kotlin coroutines. Each token is delivered via [GenStream.onDelta] as it's generated.
     *
     * **Why manual stop sequence checking?**
     * LlamaBridge does not automatically recognise Gemma's `<end_of_turn>` token as a stop signal.
     * Without this check the model would continue generating past the end of its response —
     * potentially repeating the prompt or hallucinating extra content. We cancel generation
     * immediately when any stop token is detected and return whatever was accumulated so far.
     *
     * **Why coroutine cancellation support?**
     * If the calling coroutine is cancelled (e.g. the user navigates away), we call
     * [LlamaBridge.nativeCancelGenerate] to stop inference immediately and free the CPU.
     */
    private suspend fun generateInternal(prompt: String): String = suspendCancellableCoroutine { cont ->
        val output = StringBuilder()
        val callback = object : GenStream {
            override fun onDelta(text: String) {
                // Gemma uses <end_of_turn> to signal the end of its response.
                // We also guard against "user" / "User:" in case the model starts
                // echoing the next turn of the conversation instead of stopping.
                if (text.contains("<end_of_turn>") || text.contains("user") || text.contains("User:")) {
                    LlamaBridge.nativeCancelGenerate()
                    if (cont.isActive) cont.resume(output.toString())
                    return
                }
                output.append(text)
            }

            override fun onComplete() {
                if (cont.isActive) cont.resume(output.toString())
            }

            override fun onError(message: String) {
                if (cont.isActive) cont.resumeWithException(Exception(message))
            }
        }

        LlamaBridge.generateStream(prompt, callback)

        // If the coroutine is cancelled externally, stop native inference immediately
        cont.invokeOnCancellation {
            LlamaBridge.nativeCancelGenerate()
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    /**
     * Builds a structured prompt using Gemma's chat template format.
     *
     * **Why this structure?**
     * Gemma is fine-tuned to follow the `<start_of_turn>user ... <end_of_turn>` / `<start_of_turn>model`
     * pattern. Using this format (rather than a plain string) significantly improves instruction
     * following and JSON output reliability.
     *
     * **Why ask for JSON?**
     * Structured JSON output lets us reliably extract fields (date, time, category, tags)
     * without fragile regex on free-form text. The field list mirrors [GemmaResult].
     *
     * **Why list the categories explicitly?**
     * Without an explicit list, the model may invent category names. Constraining it to
     * [ACADEMIC, PERSONAL, EVENT, INFO, SCHEDULE_CHANGE, OTHER] keeps output predictable
     * and directly mappable to [ReminderCategory].
     */
    private fun buildPrompt(message: String): String = """
        <start_of_turn>user
        You are a reminder extraction assistant for a college student app.
        Analyze the WhatsApp message below and return ONLY a valid JSON object — no extra text.

        Message: "$message"

        JSON format:
        {
          "is_reminder": true or false,
          "category": one of [ACADEMIC, PERSONAL, EVENT, INFO, SCHEDULE_CHANGE, OTHER],
          "date": "new or only date string, or null",
          "original_date": "for SCHEDULE_CHANGE only: the old date being replaced, or null",
          "time": "extracted time string or null",
          "tags": ["tag1", "tag2"]
        }
        <end_of_turn>
        <start_of_turn>model
    """.trimIndent()

    // ── Response parser ───────────────────────────────────────────────────────

    /**
     * Parses the raw model output into a [GemmaResult].
     * Returns null if the JSON is malformed (caller falls back to keyword rules).
     *
     * **Why strip markdown fences?**
     * Even when explicitly told not to, Gemma sometimes wraps its JSON in ` ```json ... ``` `
     * code fences. We strip these defensively before parsing.
     *
     * **Why search for `{` / `}` boundaries?**
     * The model may prepend a short explanation before the JSON (e.g. "Here is the result:").
     * Rather than failing, we locate the first `{` and last `}` to extract just the JSON block.
     * If no valid block is found we return null and let the caller fall back to [KeywordExtractor].
     *
     * **Why use `optString` / `optBoolean` instead of `getString`?**
     * These methods return a default value instead of throwing if a key is missing or null,
     * making parsing resilient to partially-formed model responses.
     */
    private fun parseResponse(raw: String): GemmaResult? {
        return try {
            // Strip markdown code fences the model may have added despite instructions
            val json = raw
                .replace(Regex("```json|```"), "")
                .trim()
                .let { text ->
                    // Find the outermost JSON object — ignore any preamble text
                    val start = text.indexOf('{')
                    val end   = text.lastIndexOf('}')
                    if (start != -1 && end != -1 && end > start) {
                        text.substring(start, end + 1)
                    } else {
                        null
                    }
                } ?: return null // No valid JSON block found — caller will use fallback

            val obj        = JSONObject(json)
            val isReminder = obj.optBoolean("is_reminder", false)
            val category   = parseCategory(obj.optString("category", "OTHER"))
            val dateText   = obj.optString("date").takeIf { it.isNotBlank() && it != "null" }
            val timeText   = obj.optString("time").takeIf { it.isNotBlank() && it != "null" }

            val tagsArray  = obj.optJSONArray("tags")
            val tags       = buildList {
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) add(tagsArray.getString(i))
                }
            }

            GemmaResult(
                isReminder       = isReminder,
                category         = category,
                dateText         = dateText,
                originalDateText = obj.optString("original_date").takeIf { it.isNotBlank() && it != "null" },
                timeText         = timeText,
                tags             = tags,
                fromFallback     = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse failed: ${e.message}")
            null
        }
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    /** Rule-based fallback using the existing KeywordExtractor. */
    private fun fallback(message: String) = GemmaResult(
        isReminder   = KeywordExtractor.isRelevant(message),
        category     = KeywordExtractor.categorize(message),
        dateText     = null,
        timeText     = null,
        tags         = KeywordExtractor.extractTags(message),
        fromFallback = true
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseCategory(raw: String): ReminderCategory =
        ReminderCategory.entries.firstOrNull {
            it.name.equals(raw.trim(), ignoreCase = true)
        } ?: ReminderCategory.OTHER

    fun close() {
        LlamaBridge.shutdown()
        isReady = false
    }

    companion object {
        private const val TAG = "FunctionGemma"
        private var isReady: Boolean = false
    }
}
