package com.example.myapplication.data.update

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

/**
 * Pure Kotlin/Java implementation of the binary patch application algorithm
 * (GZDIFF40 / BSDIFF standard format).
 *
 * Uses Colin Percival's suffix-based diffing algorithm adapted to use GZIP
 * compression for native, zero-dependency streaming decompression on Android.
 *
 * Format:
 *  - 0..7:   Magic ("GZDIFF40")
 *  - 8..15:  ctrl_len (little-endian Long)
 *  - 16..23: diff_len (little-endian Long)
 *  - 24..31: new_size (little-endian Long)
 *  - Followed by 3 GZIP streams: Control block, Diff block, Extra block.
 */
object BsPatch {

    private const val MAGIC = "GZDIFF40"
    private const val HEADER_SIZE = 32
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB chunk buffer

    class PatchException(message: String, cause: Throwable? = null) : IOException(message, cause)

    /**
     * Applies [patchFile] to [sourceFile], producing [targetFile].
     *
     * @param sourceFile The currently installed APK or base file.
     * @param targetFile The destination file to write the updated APK to.
     * @param patchFile The delta patch file.
     * @param onProgress Optional callback invoked with progress in [0.0, 1.0].
     * @throws PatchException if the patch is invalid, corrupted, or application fails.
     */
    @Throws(IOException::class)
    fun applyPatch(
        sourceFile: File,
        targetFile: File,
        patchFile: File,
        onProgress: ((Float) -> Unit)? = null
    ) {
        if (!sourceFile.exists() || !sourceFile.canRead()) {
            throw PatchException("Source file does not exist or is unreadable: ${sourceFile.absolutePath}")
        }
        if (!patchFile.exists() || !patchFile.canRead()) {
            throw PatchException("Patch file does not exist or is unreadable: ${patchFile.absolutePath}")
        }

        // Ensure parent directory of target exists
        targetFile.parentFile?.mkdirs()

        // Read 32-byte header
        val headerBytes = ByteArray(HEADER_SIZE)
        FileInputStream(patchFile).use { fis ->
            val read = fis.read(headerBytes)
            if (read < HEADER_SIZE) {
                throw PatchException("Corrupt patch file: header too short ($read bytes)")
            }
        }

        val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val magicBytes = ByteArray(8)
        headerBuffer.get(magicBytes)
        val magicStr = String(magicBytes, Charsets.US_ASCII)
        if (magicStr != MAGIC) {
            throw PatchException("Unsupported patch format magic: '$magicStr' (expected '$MAGIC')")
        }

        val ctrlLen = headerBuffer.long
        val diffLen = headerBuffer.long
        val newSize = headerBuffer.long

        if (ctrlLen < 0 || diffLen < 0 || newSize < 0) {
            throw PatchException("Invalid negative lengths in patch header: ctrl=$ctrlLen, diff=$diffLen, newSize=$newSize")
        }

        val totalPatchLen = patchFile.length()
        val expectedMin = HEADER_SIZE + ctrlLen + diffLen
        if (totalPatchLen < expectedMin) {
            throw PatchException("Patch file truncated: size=$totalPatchLen, expected at least $expectedMin")
        }

        // Open streams for patch blocks
        var targetOutputStream: BufferedOutputStream? = null
        var randomAccessOld: RandomAccessFile? = null
        var ctrlGzStream: DataInputStream? = null
        var diffGzStream: InputStream? = null
        var extraGzStream: InputStream? = null

        try {
            // Memory or bounded stream for the patch blocks
            val patchFis = BufferedInputStream(FileInputStream(patchFile))
            // Skip 32-byte header
            var skipped = 0L
            while (skipped < HEADER_SIZE) {
                val s = patchFis.skip(HEADER_SIZE - skipped)
                if (s <= 0) break
                skipped += s
            }

            // Read ctrl bytes
            val ctrlRaw = ByteArray(ctrlLen.toInt())
            readFully(patchFis, ctrlRaw)
            ctrlGzStream = DataInputStream(GZIPInputStream(ByteArrayInputStream(ctrlRaw)))

            // Read diff bytes
            val diffRaw = ByteArray(diffLen.toInt())
            readFully(patchFis, diffRaw)
            diffGzStream = GZIPInputStream(ByteArrayInputStream(diffRaw))

            // Extra bytes are the remainder of the file
            extraGzStream = GZIPInputStream(patchFis)

            targetOutputStream = BufferedOutputStream(FileOutputStream(targetFile), BUFFER_SIZE)
            randomAccessOld = RandomAccessFile(sourceFile, "r")

            var oldPos = 0L
            var newPos = 0L
            val oldLen = sourceFile.length()

            val diffBuf = ByteArray(BUFFER_SIZE)
            val oldBuf = ByteArray(BUFFER_SIZE)
            val extraBuf = ByteArray(BUFFER_SIZE)

            while (newPos < newSize) {
                // Read 3 x 8-byte ints (24 bytes) little-endian from ctrl block:
                // diff_len, extra_len, seek_offset
                val tupleBuf = ByteArray(24)
                try {
                    ctrlGzStream.readFully(tupleBuf)
                } catch (e: Exception) {
                    break
                }
                val tuple = ByteBuffer.wrap(tupleBuf).order(ByteOrder.LITTLE_ENDIAN)
                val diffCount = tuple.long
                val extraCount = tuple.long
                val seekOffset = tuple.long

                if (diffCount < 0 || extraCount < 0) {
                    throw PatchException("Corrupt patch control block: diffCount=$diffCount, extraCount=$extraCount")
                }
                if (newPos + diffCount + extraCount > newSize) {
                    throw PatchException("Patch writes beyond target file bounds: $newPos + $diffCount + $extraCount > $newSize")
                }

                // 1. Process Diff Block (add bytes from old file to diff stream modulo 256)
                var remainingDiff = diffCount
                while (remainingDiff > 0) {
                    val toRead = minOf(remainingDiff, BUFFER_SIZE.toLong()).toInt()
                    readFully(diffGzStream, diffBuf, 0, toRead)

                    // Read corresponding slice from old file if within range
                    val bytesFromOld = if (oldPos < oldLen) {
                        randomAccessOld.seek(oldPos)
                        val avail = minOf(toRead.toLong(), oldLen - oldPos).toInt()
                        randomAccessOld.read(oldBuf, 0, avail)
                    } else {
                        0
                    }

                    for (i in 0 until toRead) {
                        val oldVal = if (i < bytesFromOld) oldBuf[i].toInt() and 0xFF else 0
                        val diffVal = diffBuf[i].toInt() and 0xFF
                        val res = (oldVal + diffVal) and 0xFF
                        diffBuf[i] = res.toByte()
                    }

                    targetOutputStream.write(diffBuf, 0, toRead)
                    newPos += toRead
                    oldPos += toRead
                    remainingDiff -= toRead
                }

                // 2. Process Extra Block (literal bytes copied directly to target)
                var remainingExtra = extraCount
                while (remainingExtra > 0) {
                    val toRead = minOf(remainingExtra, BUFFER_SIZE.toLong()).toInt()
                    readFully(extraGzStream, extraBuf, 0, toRead)
                    targetOutputStream.write(extraBuf, 0, toRead)
                    newPos += toRead
                    remainingExtra -= toRead
                }

                // 3. Adjust old file pointer
                oldPos += seekOffset

                // Progress callback
                if (newSize > 0) {
                    onProgress?.invoke(minOf(1.0f, newPos.toFloat() / newSize.toFloat()))
                }
            }

            targetOutputStream.flush()

            if (newPos != newSize) {
                throw PatchException("Reconstructed file size mismatch: got $newPos bytes, expected $newSize bytes")
            }

            onProgress?.invoke(1.0f)

        } catch (e: Exception) {
            // Clean up incomplete target file on failure
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (e is PatchException) throw e
            throw PatchException("Error applying patch: ${e.message}", e)
        } finally {
            ctrlGzStream?.close()
            diffGzStream?.close()
            extraGzStream?.close()
            randomAccessOld?.close()
            targetOutputStream?.close()
        }
    }

    private fun readFully(stream: InputStream, b: ByteArray, off: Int = 0, len: Int = b.size) {
        var n = 0
        while (n < len) {
            val count = stream.read(b, off + n, len - n)
            if (count < 0) {
                throw PatchException("Unexpected end of patch stream after $n bytes (expected $len)")
            }
            n += count
        }
    }
}
