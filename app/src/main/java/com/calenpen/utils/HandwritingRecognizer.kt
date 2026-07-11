package com.calenpen.utils

import android.graphics.PointF
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.digitalink.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper around ML Kit Digital Ink Recognition for converting on-screen handwriting
 * strokes into recognised text.
 *
 * Usage:
 * ```kotlin
 * val recogniser = HandwritingRecognizer()
 * recogniser.downloadModelIfNeeded("en-US")
 * val text = recogniser.recognise(strokes, "en-US")
 * ```
 */
class HandwritingRecognizer {

    /**
     * Recognises handwriting from a list of [RecognitionStroke] objects.
     *
     * @param strokes  Ordered list of strokes that make up the handwritten content.
     * @param languageTag  BCP-47 language tag, e.g. "en-US", "de-DE".
     * @return The best recognition candidate, or an empty string if recognition fails.
     */
    suspend fun recognise(
        strokes: List<RecognitionStroke>,
        languageTag: String = "en-US"
    ): String = suspendCancellableCoroutine { cont ->
        val modelIdentifier = try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        } catch (e: MlKitException) {
            cont.resume("")
            return@suspendCancellableCoroutine
        } ?: run {
            cont.resume("")
            return@suspendCancellableCoroutine
        }

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val recogniser = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )

        val inkBuilder = Ink.builder()
        strokes.forEach { stroke ->
            val strokeBuilder = Ink.Stroke.builder()
            stroke.points.forEach { pt ->
                strokeBuilder.addPoint(Ink.Point.create(pt.x, pt.y, pt.timestamp))
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }

        recogniser.recognize(inkBuilder.build())
            .addOnSuccessListener { result ->
                val text = result.candidates.firstOrNull()?.text ?: ""
                cont.resume(text)
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
    }

    /**
     * Downloads the recognition model for the given language if it is not already available.
     *
     * @param languageTag  BCP-47 language tag.
     */
    suspend fun downloadModelIfNeeded(languageTag: String = "en-US") {
        val modelIdentifier = try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        } catch (e: MlKitException) {
            return
        } ?: return

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val modelManager = RemoteModelManager.getInstance()

        suspendCancellableCoroutine<Unit> { cont ->
            modelManager.download(model, com.google.mlkit.common.model.DownloadConditions.Builder().build())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) } // non-fatal: proceed with whatever is cached
        }
    }

    /**
     * Returns true if the recognition model for [languageTag] is already downloaded.
     */
    suspend fun isModelDownloaded(languageTag: String = "en-US"): Boolean {
        val modelIdentifier = try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        } catch (e: MlKitException) {
            return false
        } ?: return false

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val modelManager = RemoteModelManager.getInstance()

        return suspendCancellableCoroutine { cont ->
            modelManager.isModelDownloaded(model)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(false) }
        }
    }
}

/**
 * Represents a single stroke to be submitted for recognition.
 *
 * @property points  Ordered list of sample points.
 */
data class RecognitionStroke(val points: List<StrokePoint>)

/** A sampled point within a stroke. */
data class StrokePoint(
    val x: Float,
    val y: Float,
    /** Timestamp in milliseconds since the pen was first placed on the page. */
    val timestamp: Long
)

/** Convenience extension to convert a [PointF] into a [StrokePoint]. */
fun PointF.toStrokePoint(timestamp: Long) = StrokePoint(x, y, timestamp)
