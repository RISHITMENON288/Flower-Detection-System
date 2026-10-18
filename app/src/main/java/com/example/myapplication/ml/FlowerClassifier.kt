package com.example.myapplication.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.myapplication.data.FlowerLabels
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream

data class Prediction(val label: String, val confidence: Float)

private const val INPUT_SIZE = 224
private const val RESIZE_SIZE = 256
private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

/**
 * Wraps the trained flower_model.ptl. Knows nothing about Compose or UI —
 * only how to load the model and run inference on a Bitmap. Owned by
 * HomeViewModel; never call this directly from a composable.
 *
 * Preprocessing mirrors train.ipynb's val_transforms exactly:
 * Resize(256) -> CenterCrop(224) -> ToTensor -> Normalize(ImageNet stats).
 */
class FlowerClassifier(context: Context) {

    private val module: Module = LiteModuleLoader.load(assetFilePath(context, "flower_model.ptl"))

    /** Guards only the native forward() call. PyTorch's Module isn't
     * documented as safe for concurrent invocation from multiple threads,
     * but preprocessing (bitmap scale/crop/tensor conversion) and softmax
     * are pure CPU work with no shared native state, so they stay outside
     * the lock — holding it only around the single call that actually
     * touches the native module keeps this from becoming a bottleneck. */
    private val inferenceLock = Any()

    /** Runs inference and returns the top [topK] predictions, highest confidence first. */
    fun classify(bitmap: Bitmap, topK: Int = 5): List<Prediction> {
        val inputTensor = preprocess(bitmap)
        val outputTensor = synchronized(inferenceLock) {
            module.forward(IValue.from(inputTensor)).toTensor()
        }
        val scores = softmax(outputTensor.dataAsFloatArray)

        return scores
            .withIndex()
            .sortedByDescending { it.value }
            .take(topK)
            .map { (index, score) -> Prediction(FlowerLabels.NAMES[index], score) }
    }

    /** Releases the native module. Call once, when the owning component
     * (HomeViewModel) is cleared — never mid-classification. */
    fun close() {
        module.destroy()
    }

    private fun preprocess(bitmap: Bitmap): Tensor {
        val scale = RESIZE_SIZE.toFloat() / minOf(bitmap.width, bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(INPUT_SIZE)
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(INPUT_SIZE)
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val x = (scaled.width - INPUT_SIZE) / 2
        val y = (scaled.height - INPUT_SIZE) / 2
        val cropped = Bitmap.createBitmap(scaled, x, y, INPUT_SIZE, INPUT_SIZE)

        return TensorImageUtils.bitmapToFloat32Tensor(cropped, MEAN, STD)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val exps = logits.map { Math.exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return exps.map { it / sum }.toFloatArray()
    }

    companion object {
        /** Module.load needs a filesystem path, so the asset is copied to
         * internal storage once and reused on every later launch. */
        private fun assetFilePath(context: Context, assetName: String): String {
            val file = File(context.filesDir, assetName)
            if (file.exists() && file.length() > 0) return file.absolutePath

            context.assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file.absolutePath
        }
    }
}
