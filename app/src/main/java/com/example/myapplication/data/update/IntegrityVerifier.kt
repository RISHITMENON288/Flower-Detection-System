package com.example.myapplication.data.update

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale

/**
 * Cryptographic hash verification and formatting utility for delta and full update assets.
 */
object IntegrityVerifier {

    private const val BUFFER_SIZE = 64 * 1024 // 64 KB

    /**
     * Computes the hexadecimal lowercase SHA-256 checksum of [file].
     */
    @Throws(IOException::class)
    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val sb = StringBuilder(hashBytes.size * 2)
        for (b in hashBytes) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    /**
     * Verifies whether [file]'s SHA-256 checksum matches [expectedHash].
     * Returns true if expectedHash is null/blank or matches case-insensitively.
     */
    fun verifySha256(file: File, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return true
        return try {
            val actual = computeSha256(file)
            actual.equals(expectedHash.trim(), ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Formats bytes into a human-readable string (e.g. "2.4 MB", "512 KB").
     */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    /**
     * Calculates the bandwidth savings percentage of patch size relative to full size.
     */
    fun calculateSavingsPercent(patchSize: Long, fullSize: Long): Int {
        if (fullSize <= 0L || patchSize >= fullSize) return 0
        val saved = fullSize - patchSize
        return ((saved * 100) / fullSize).toInt().coerceIn(0, 99)
    }
}
