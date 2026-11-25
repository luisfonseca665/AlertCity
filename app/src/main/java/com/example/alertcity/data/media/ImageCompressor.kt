package com.example.alertcity.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val MAX_IMAGE_DIM = 1280
    private const val JPEG_QUALITY = 80
    private const val PHOTOS_DIR = "reportes_fotos"

    /**
     * Lee la imagen de cámara o galería, corrige orientación EXIF, escala a máximo 1600px
     * y comprime en JPEG dentro de filesDir/reportes_fotos para uso offline y subida diferida.
     */
    suspend fun compressAndSaveToInternalStorage(context: Context, sourceUri: Uri): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val destDir = File(context.filesDir, PHOTOS_DIR).apply { if (!exists()) mkdirs() }
                val destFile = File(destDir, "foto_${System.currentTimeMillis()}.jpg")
                if (compressUriToFile(context, sourceUri, destFile)) {
                    Uri.fromFile(destFile)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error comprimiendo imagen para almacenamiento interno", e)
                null
            }
        }

    /**
     * Prepara un archivo listo para MultipartBody.Part. Si el archivo ya fue comprimido en
     * filesDir/reportes_fotos, lo reutiliza directamente sin recomprimir; de lo contrario crea un
     * archivo temporal comprimido que el llamador debe borrar tras subir.
     */
    suspend fun prepareMultipartPart(
        context: Context,
        uriString: String
    ): Pair<MultipartBody.Part, File?>? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" || uri.scheme.isNullOrBlank()) {
                val filePath = if (uri.scheme == "file") uri.path else uriString
                val existingFile = filePath?.let { File(it) }
                if (existingFile != null && existingFile.exists() && existingFile.length() > 0L) {
                    val isManagedPhoto = existingFile.parentFile?.name == PHOTOS_DIR
                    if (isManagedPhoto) {
                        val requestBody = existingFile.asRequestBody("image/jpeg".toMediaType())
                        val part = MultipartBody.Part.createFormData("file", existingFile.name, requestBody)
                        return@withContext part to null
                    }
                }
            }

            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            if (!compressUriToFile(context, uri, tempFile)) {
                tempFile.delete()
                return@withContext null
            }

            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
            part to tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error preparando imagen multipart", e)
            null
        }
    }

    private fun compressUriToFile(context: Context, sourceUri: Uri, destFile: File): Boolean {
        val orientation = readExifOrientation(context, sourceUri)

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openUriStream(context, sourceUri) ?: return false
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return false
        }

        val sampleSize = calculateInSampleSize(boundsOptions, MAX_IMAGE_DIM, MAX_IMAGE_DIM)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decodedBitmap = openUriStream(context, sourceUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return false

        val orientedBitmap = applyExifOrientation(decodedBitmap, orientation)
        if (orientedBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }

        val finalBitmap = scaleBitmapIfNeeded(orientedBitmap, MAX_IMAGE_DIM)
        if (finalBitmap !== orientedBitmap) {
            orientedBitmap.recycle()
        }

        return try {
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            true
        } finally {
            finalBitmap.recycle()
        }
    }

    private fun openUriStream(context: Context, uri: Uri): InputStream? {
        return when (uri.scheme) {
            "file" -> uri.path?.let { File(it).takeIf(File::exists)?.inputStream() }
            else -> context.contentResolver.openInputStream(uri)
        }
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return try {
            openUriStream(context, uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (width >= height) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
