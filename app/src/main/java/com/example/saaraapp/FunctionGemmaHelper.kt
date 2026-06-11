package com.example.saaraapp

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Wraps MediaPipe LLM Inference to run FunctionGemma 270M on-device.
 *
 * Usage:
 *   1. Call [initialize] once after the model file is downloaded.
 *   2. Call [analyze] for each WhatsApp message.
 *   3. Call [close] when done (e.g. in onDestroy).
 *
 * If the model is not yet ready, [analyze] automatically falls back to
 * the rule-based [KeywordExtractor] so the app always works.
 */
class FunctionGemmaHelper(private val context: Context) {

    private var llmInference: LlmInference? = null

    val isReady: Boolean get() = llmInference != null

    // ── Initialise ────────────────────────────────────────────────────────────

    /**
     * Loads the model from [modelFile]. Call this once the download is complete.
     * Runs on IO thread — safe to call from a coroutine.
     */
    suspend fun initialize(modelFile: File) = withContext(Dispatchers.IO) {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(256)          // enough for a short JSON reply
                .setTopK(1)                 // greedy decoding → deterministic JSON
                .setTemperature(0.1f)       // low temp → structured, consistent output
                .setRandomSeed(42)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.i(TAG, "FunctionGemma loaded from ${modelFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load FunctionGemma: ${e.message}")
            llmInference = null
        }
    }

    // ── Analyze ───────────────────────────────────────────────────────────────

    /**
     * Analyzes [message] and returns a [GemmaResult].
     *
     * - If the model IS ready  → uses FunctionGemma (smart, context-aware)
     * - If the model is NOT ready → falls back to KeywordExtractor (rule-based)
     */
    suspend fun analyze(message: String): GemmaResult = withContext(Dispatchers.Default) {
        val model = llmInference
        if (model == null) {
            Log.d(TAG, "Model not ready — using KeywordExtractor fallback")
            return@withContext fallback(message)
        }

        return@withContext try {
            val prompt   = buildPrompt(message)
            val response = model.generateResponse(prompt)
            parseResponse(response) ?: fallback(message)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message} — using fallback")
            fallback(message)
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    /**
     * Builds a Gemma function-calling prompt.
     * The <start_of_turn> / <end_of_turn> tokens are Gemma's chat template.
     */
    private fun buildPrompt(message: String): String = """
        <start_of_turn>user
        You are a reminder extraction assistant for a college student app.
        Analyze the WhatsApp message below and return ONLY a valid JSON object — no extra text.

        Message: "$message"

        JSON format:
        {
          "is_reminder": true or false,
          "category": one of [DEADLINE, ASSIGNMENT, EXAM, MEETING, REMINDER, SCHEDULE_CHANGE, HOLIDAY, OTHER],
          "date": "extracted date string or null",
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
     */
    private fun parseResponse(raw: String): GemmaResult? {
        return try {
            // Strip any markdown code fences the model might add
            val json = raw
                .replace(Regex("```json|```"), "")
                .trim()
                // Grab the first { ... } block
                .let { text ->
                    val start = text.indexOf('{')
                    val end   = text.lastIndexOf('}')
                    if (start != -1 && end != -1) text.substring(start, end + 1) else text
                }

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
                isReminder   = isReminder,
                category     = category,
                dateText     = dateText,
                timeText     = timeText,
                tags         = tags,
                fromFallback = false
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
        dateText     = null,   // DateParser still handles dates in the service
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
        llmInference?.close()
        llmInference = null
    }

    companion object {
        private const val TAG = "FunctionGemma"
    }
}
