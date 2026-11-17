package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import coil.disk.DiskCache
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

internal const val MAX_FULLSCREEN_BASE_DIMENSION = 4096
internal const val SUBSAMPLED_TILE_OUTPUT_PX = 768

internal data class TileKey(
    val col: Int,
    val row: Int,
    val sampleSize: Int
)

internal data class TileSpec(
    val key: TileKey,
    val srcRect: Rect
)

private data class TileBitmapEntry(
    val imageBitmap: ImageBitmap,
    val byteSize: Int
)

/**
 * Picks the optimal power-of-2 [BitmapRegionDecoder] sample size for the current zoom level.
 * Returns null when the decoded base bitmap already provides sufficient resolution.
 */
internal fun computeSubsampleSize(
    originalWidth: Int,
    baseWidth: Int,
    scaledWidthOnScreen: Float
): Int? {
    if (originalWidth <= 0 || baseWidth <= 0) return null
    if (originalWidth <= baseWidth || scaledWidthOnScreen <= baseWidth * 0.75f) {
        return null
    }
    val targetDecodedWidth = (scaledWidthOnScreen * 1.25f).coerceAtLeast(baseWidth.toFloat())
    var sample = 1
    while (originalWidth / (sample * 2) >= targetDecodedWidth &&
        originalWidth / (sample * 2) > baseWidth
    ) {
        sample *= 2
    }
    return sample
}

/**
 * Computes the list of visible [TileSpec] items intersecting the viewport.
 */
internal fun computeVisibleTiles(
    origW: Int,
    origH: Int,
    containerW: Float,
    containerH: Float,
    scaledW: Float,
    scaledH: Float,
    imgLeft: Float,
    imgTop: Float,
    sampleSize: Int,
    tileOutputPx: Int = SUBSAMPLED_TILE_OUTPUT_PX
): List<TileSpec> {
    if (origW <= 0 || origH <= 0 || containerW <= 0f || containerH <= 0f ||
        scaledW <= 0f || scaledH <= 0f || sampleSize <= 0 || tileOutputPx <= 0
    ) {
        return emptyList()
    }

    val visOrigLeft = (((0f - imgLeft) / scaledW) * origW).coerceIn(0f, origW.toFloat())
    val visOrigRight = (((containerW - imgLeft) / scaledW) * origW).coerceIn(0f, origW.toFloat())
    val visOrigTop = (((0f - imgTop) / scaledH) * origH).coerceIn(0f, origH.toFloat())
    val visOrigBottom = (((containerH - imgTop) / scaledH) * origH).coerceIn(0f, origH.toFloat())

    if (visOrigRight <= visOrigLeft || visOrigBottom <= visOrigTop) {
        return emptyList()
    }

    val tileOrigStep = (tileOutputPx * sampleSize).coerceAtLeast(1)
    val maxCol = ((origW - 1) / tileOrigStep).coerceAtLeast(0)
    val maxRow = ((origH - 1) / tileOrigStep).coerceAtLeast(0)

    val startCol = (visOrigLeft.toInt() / tileOrigStep).coerceIn(0, maxCol)
    val endCol = ((kotlin.math.ceil(visOrigRight.toDouble()).toInt() - 1).coerceAtLeast(0) / tileOrigStep)
        .coerceIn(0, maxCol)
    val startRow = (visOrigTop.toInt() / tileOrigStep).coerceIn(0, maxRow)
    val endRow = ((kotlin.math.ceil(visOrigBottom.toDouble()).toInt() - 1).coerceAtLeast(0) / tileOrigStep)
        .coerceIn(0, maxRow)

    val result = ArrayList<TileSpec>((endCol - startCol + 1) * (endRow - startRow + 1))
    for (row in startRow..endRow) {
        val top = row * tileOrigStep
        val bottom = minOf(origH, top + tileOrigStep)
        for (col in startCol..endCol) {
            val left = col * tileOrigStep
            val right = minOf(origW, left + tileOrigStep)
            if (right > left && bottom > top) {
                result.add(
                    TileSpec(
                        key = TileKey(col = col, row = row, sampleSize = sampleSize),
                        srcRect = Rect(left, top, right, bottom)
                    )
                )
            }
        }
    }
    return result
}

internal class SubsampledImageTileRenderer(
    private val decoder: BitmapRegionDecoder,
    private val snapshot: DiskCache.Snapshot?
) {
    val originalWidth: Int = decoder.width
    val originalHeight: Int = decoder.height

    private val decodeMutex = Mutex()

    @Volatile
    private var isClosed = false

    private val maxCacheBytes = minOf(
        64 * 1024 * 1024,
        (Runtime.getRuntime().maxMemory() / 6L).toInt().coerceAtLeast(16 * 1024 * 1024)
    )

    private val tileCache = object : LruCache<TileKey, TileBitmapEntry>(maxCacheBytes) {
        override fun sizeOf(key: TileKey, value: TileBitmapEntry): Int = value.byteSize
    }

    fun getCachedTile(key: TileKey): ImageBitmap? {
        if (isClosed) return null
        return tileCache.get(key)?.imageBitmap
    }

    suspend fun decodeTileIfNeeded(spec: TileSpec): Boolean {
        if (isClosed || tileCache.get(spec.key) != null) return false
        return withContext(Dispatchers.IO) {
            decodeMutex.withLock {
                if (isClosed || tileCache.get(spec.key) != null) {
                    return@withLock false
                }
                try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = spec.key.sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bitmap = decoder.decodeRegion(spec.srcRect, options) ?: return@withLock false
                    val entry = TileBitmapEntry(
                        imageBitmap = bitmap.asImageBitmap(),
                        byteSize = bitmap.allocationByteCount.coerceAtLeast(1)
                    )
                    tileCache.put(spec.key, entry)
                    true
                } catch (_: Throwable) {
                    false
                }
            }
        }
    }

    suspend fun close() {
        isClosed = true
        withContext(Dispatchers.IO) {
            decodeMutex.withLock {
                tileCache.evictAll()
                try {
                    if (!decoder.isRecycled) {
                        decoder.recycle()
                    }
                } catch (_: Throwable) {
                }
                try {
                    snapshot?.close()
                } catch (_: Throwable) {
                }
            }
        }
    }
}

internal suspend fun openSubsampledTileRenderer(
    context: Context,
    imageRequest: ImageRequest,
    baseDecodedSize: IntSize
): SubsampledImageTileRenderer? = withContext(Dispatchers.IO) {
    if (baseDecodedSize.width <= 0 || baseDecodedSize.height <= 0) return@withContext null

    var snapshot: DiskCache.Snapshot? = null
    try {
        val data = imageRequest.data
        val localFile: File? = when (data) {
            is File -> data.takeIf { it.exists() && it.isFile }
            is Uri -> if (data.scheme == "file") data.path?.let(::File)?.takeIf { it.exists() } else null
            is String -> {
                val parsed = Uri.parse(data)
                if (parsed.scheme == "file") {
                    parsed.path?.let(::File)?.takeIf { it.exists() }
                } else {
                    null
                }
            }
            else -> null
        }

        val contentUri: Uri? = when (data) {
            is Uri -> data.takeIf { it.scheme == "content" }
            is String -> Uri.parse(data).takeIf { it.scheme == "content" }
            else -> null
        }

        val decoder: BitmapRegionDecoder? = when {
            localFile != null -> {
                if (!hasNormalExifOrientation(localFile)) return@withContext null
                ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    BitmapRegionDecoder.newInstance(pfd)
                }
            }
            contentUri != null -> {
                context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                    val orientation = try {
                        ExifInterface(pfd.fileDescriptor).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } catch (_: Throwable) {
                        ExifInterface.ORIENTATION_NORMAL
                    }
                    if (orientation != ExifInterface.ORIENTATION_NORMAL &&
                        orientation != ExifInterface.ORIENTATION_UNDEFINED
                    ) {
                        return@withContext null
                    }
                    BitmapRegionDecoder.newInstance(pfd)
                }
            }
            else -> {
                val diskKey = imageRequest.diskCacheKey ?: data.toString()
                snapshot = context.imageLoader.diskCache?.openSnapshot(diskKey)
                val cachedFile = snapshot?.data?.toFile()
                if (cachedFile != null && cachedFile.exists() && hasNormalExifOrientation(cachedFile)) {
                    ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        BitmapRegionDecoder.newInstance(pfd)
                    }
                } else {
                    null
                }
            }
        }

        if (decoder == null || decoder.width <= 0 || decoder.height <= 0) {
            decoder?.recycle()
            snapshot?.close()
            return@withContext null
        }

        // Only allocate region tile renderer when original file has higher resolution than base bitmap
        if (decoder.width <= baseDecodedSize.width && decoder.height <= baseDecodedSize.height) {
            decoder.recycle()
            snapshot?.close()
            return@withContext null
        }

        SubsampledImageTileRenderer(decoder = decoder, snapshot = snapshot)
    } catch (_: Throwable) {
        try {
            snapshot?.close()
        } catch (_: Throwable) {
        }
        null
    }
}

private fun hasNormalExifOrientation(file: File): Boolean {
    return try {
        val orientation = ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED
    } catch (_: Throwable) {
        true
    }
}
