package com.extremestudios.portalarekmusik.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SelectedImagePayload(
    val base64: String,
    val fileName: String,
    val mimeType: String,
    val previewBitmap: Bitmap
)

data class SelectedFilePayload(
    val base64: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

suspend fun readSelectedImage(
    context: Context,
    uri: Uri
): Result<SelectedImagePayload> = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.ifBlank { null } ?: "image/jpeg"
        val fileName = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }?.ifBlank { null } ?: defaultFileNameForMimeType(mimeType)

        val bytes = resolver.openInputStream(uri)?.use { input -> input.readBytes() }
            ?: throw IllegalStateException("Gambar tidak bisa dibaca.")
        val previewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Preview gambar gagal dibuat.")
        val processedImage = prepareImagePayload(
            originalBytes = bytes,
            originalMimeType = mimeType,
            originalFileName = fileName,
            previewBitmap = previewBitmap
        )

        SelectedImagePayload(
            base64 = Base64.encodeToString(processedImage.bytes, Base64.NO_WRAP),
            fileName = processedImage.fileName,
            mimeType = processedImage.mimeType,
            previewBitmap = processedImage.previewBitmap
        )
    }.recoverCatching { error ->
        throw IllegalStateException(error.message ?: "Gagal memproses gambar.")
    }
}

private fun defaultFileNameForMimeType(mimeType: String): String = when (mimeType.lowercase()) {
    "image/png" -> "image.png"
    "image/webp" -> "image.webp"
    else -> "image.jpg"
}

private data class ProcessedImagePayload(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val previewBitmap: Bitmap
)

private fun prepareImagePayload(
    originalBytes: ByteArray,
    originalMimeType: String,
    originalFileName: String,
    previewBitmap: Bitmap
): ProcessedImagePayload {
    val boundedBitmap = scaleBitmapIfNeeded(previewBitmap)
    val shouldPreservePng = originalMimeType.equals("image/png", ignoreCase = true) && boundedBitmap.hasAlpha()
    val outputMimeType = if (shouldPreservePng) "image/png" else "image/jpeg"
    val outputBytes = if (
        originalBytes.size <= MAX_IMAGE_UPLOAD_BYTES &&
        previewBitmap.width <= MAX_IMAGE_EDGE &&
        previewBitmap.height <= MAX_IMAGE_EDGE &&
        outputMimeType.equals(originalMimeType, ignoreCase = true)
    ) {
        originalBytes
    } else {
        ByteArrayOutputStream().use { stream ->
            val format = if (shouldPreservePng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val quality = if (shouldPreservePng) 100 else 84
            boundedBitmap.compress(format, quality, stream)
            stream.toByteArray()
        }
    }
    val outputFileName = ensureImageExtension(originalFileName, outputMimeType)
    return ProcessedImagePayload(
        bytes = outputBytes,
        fileName = outputFileName,
        mimeType = outputMimeType,
        previewBitmap = boundedBitmap
    )
}

private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
    val maxSide = maxOf(bitmap.width, bitmap.height)
    if (maxSide <= MAX_IMAGE_EDGE) return bitmap
    val ratio = MAX_IMAGE_EDGE.toFloat() / maxSide.toFloat()
    val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private fun ensureImageExtension(fileName: String, mimeType: String): String {
    val baseName = fileName.substringBeforeLast('.', fileName).ifBlank { "image" }
    val extension = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
    return "$baseName.$extension"
}

private const val MAX_IMAGE_EDGE = 1600
private const val MAX_IMAGE_UPLOAD_BYTES = 1_500_000

suspend fun readSelectedFile(
    context: Context,
    uri: Uri,
    fallbackFileName: String,
    fallbackMimeType: String,
    maxSizeBytes: Long? = null
): Result<SelectedFilePayload> = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val fileMeta = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                Pair(
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                    if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                )
            } else {
                null
            }
        }

        val resolverMimeType = resolver.getType(uri)?.ifBlank { null }
        val mimeType = when {
            fallbackMimeType.equals("audio/mpeg", ignoreCase = true) -> "audio/mpeg"
            resolverMimeType != null -> resolverMimeType
            else -> fallbackMimeType
        }
        val fileName = fileMeta?.first?.ifBlank { null } ?: fallbackFileName
        val bytes = resolver.openInputStream(uri)?.use { input -> input.readBytes() }
            ?: throw IllegalStateException("File tidak bisa dibaca.")
        val sizeBytes = if ((fileMeta?.second ?: -1L) > 0L) fileMeta!!.second else bytes.size.toLong()

        if (maxSizeBytes != null && sizeBytes > maxSizeBytes) {
            throw IllegalStateException("Ukuran MP3 maksimal 10MB. Silakan kompres dulu.")
        }

        SelectedFilePayload(
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
    }.recoverCatching { error ->
        throw IllegalStateException(error.message ?: "Gagal memproses file.")
    }
}
