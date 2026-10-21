package com.example.myapplication.data.update

import com.example.myapplication.data.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream

class DeltaUpdateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSha256Verification() {
        val testFile = tempFolder.newFile("test_sha.txt")
        testFile.writeText("FloraScan Delta Update Test Content 12345")

        val hash = IntegrityVerifier.computeSha256(testFile)
        assertTrue(hash.isNotEmpty())
        assertEquals(64, hash.length)

        // Verifying match
        assertTrue(IntegrityVerifier.verifySha256(testFile, hash))
        assertTrue(IntegrityVerifier.verifySha256(testFile, hash.uppercase()))
        assertFalse(IntegrityVerifier.verifySha256(testFile, "0000000000000000000000000000000000000000000000000000000000000000"))
        assertTrue(IntegrityVerifier.verifySha256(testFile, null))
    }

    @Test
    fun testSavingsCalculation() {
        // 2 MB patch vs 50 MB full APK
        val savings = IntegrityVerifier.calculateSavingsPercent(2 * 1024 * 1024, 50 * 1024 * 1024)
        assertEquals(96, savings)

        // Patch larger or equal to full
        assertEquals(0, IntegrityVerifier.calculateSavingsPercent(50, 50))
        assertEquals(0, IntegrityVerifier.calculateSavingsPercent(60, 50))
    }

    @Test
    fun testIsNewerVersion() {
        assertTrue(UpdateChecker.isNewerVersion("v4.1", "4.0"))
        assertTrue(UpdateChecker.isNewerVersion("4.1", "4.0"))
        assertTrue(UpdateChecker.isNewerVersion("v5.0", "4.0"))
        assertFalse(UpdateChecker.isNewerVersion("v4.0", "4.0"))
        assertFalse(UpdateChecker.isNewerVersion("4.0", "4.0"))
        assertFalse(UpdateChecker.isNewerVersion("", "4.0"))
    }

    @Test
    fun testParseManifestJson() {
        val sampleJson = """
            {
              "versionName": "4.1",
              "versionCode": 5,
              "releaseTag": "v4.1",
              "releaseUrl": "https://github.com/RISHITMENON288/Flower-Detection-System/releases/tag/v4.1",
              "changelog": "Added delta update support.",
              "fullApk": {
                "fileName": "app-release.apk",
                "downloadUrl": "https://example.com/app-release.apk",
                "sizeBytes": 52428800,
                "sha256": "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
              },
              "patches": [
                {
                  "fromVersionName": "4.0",
                  "fromVersionCode": 4,
                  "patchFileName": "patch_v4.0_to_v4.1.patch",
                  "downloadUrl": "https://example.com/patch.patch",
                  "patchSizeBytes": 2097152,
                  "patchSha256": "1122334455667788990011223344556677889900112233445566778899001122",
                  "targetSha256": "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
                }
              ]
            }
        """.trimIndent()

        val manifest = UpdateChecker.parseManifestJson(sampleJson)
        assertEquals("4.1", manifest.versionName)
        assertEquals(5, manifest.versionCode)
        assertEquals(52428800L, manifest.fullApk.sizeBytes)
        assertEquals(1, manifest.patches.size)

        val patch = manifest.patches[0]
        assertEquals("4.0", patch.fromVersionName)
        assertEquals(4, patch.fromVersionCode)
        assertEquals(2097152L, patch.patchSizeBytes)
    }

    @Test
    fun testBsPatchApplication() {
        val oldFile = tempFolder.newFile("old.bin")
        val targetFile = tempFolder.newFile("target.bin")
        val patchFile = tempFolder.newFile("test.patch")

        // Old file: 1000 bytes of repeated patterns
        val oldData = ByteArray(1000) { (it % 256).toByte() }
        oldFile.writeBytes(oldData)

        // New file: 500 bytes from old (modified by +1), then 200 bytes extra, then 500 bytes from old
        val expectedNew = ByteArray(1200)
        for (i in 0 until 500) {
            expectedNew[i] = ((oldData[i] + 1) % 256).toByte()
        }
        for (i in 500 until 700) {
            expectedNew[i] = 0x7F.toByte()
        }
        for (i in 700 until 1200) {
            expectedNew[i] = oldData[i - 200]
        }

        // Build GZDIFF40 patch manually
        // Control:
        //  Tuple 1: diffCount=500, extraCount=200, seekOffset=0 (oldPos now 500)
        //  Tuple 2: diffCount=500, extraCount=0, seekOffset=0 (oldPos 500..1000)
        val ctrlBos = ByteArrayOutputStream()
        val ctrlDos = DataOutputStream(ctrlBos)

        fun writeTuple(diffLen: Long, extraLen: Long, seekOffset: Long) {
            val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            buf.putLong(diffLen)
            buf.putLong(extraLen)
            buf.putLong(seekOffset)
            ctrlDos.write(buf.array())
        }

        writeTuple(500L, 200L, 0L)
        writeTuple(500L, 0L, 0L)
        ctrlDos.flush()

        // Diff block: 500 bytes of 1s, then 500 bytes of 0s
        val diffData = ByteArray(1000)
        for (i in 0 until 500) diffData[i] = 1
        for (i in 500 until 1000) diffData[i] = 0

        // Extra block: 200 bytes of 0x7F
        val extraData = ByteArray(200) { 0x7F.toByte() }

        fun gzCompress(bytes: ByteArray): ByteArray {
            val baos = ByteArrayOutputStream()
            GZIPOutputStream(baos).use { it.write(bytes) }
            return baos.toByteArray()
        }

        val ctrlGz = gzCompress(ctrlBos.toByteArray())
        val diffGz = gzCompress(diffData)
        val extraGz = gzCompress(extraData)

        // Write patch file
        FileOutputStream(patchFile).use { fos ->
            val header = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            header.put("GZDIFF40".toByteArray(Charsets.US_ASCII))
            header.putLong(ctrlGz.size.toLong())
            header.putLong(diffGz.size.toLong())
            header.putLong(1200L) // newSize
            fos.write(header.array())
            fos.write(ctrlGz)
            fos.write(diffGz)
            fos.write(extraGz)
        }

        // Apply patch
        var progressReported = 0f
        BsPatch.applyPatch(oldFile, targetFile, patchFile) { progress ->
            progressReported = progress
        }

        assertEquals(1.0f, progressReported, 0.01f)
        assertEquals(1200L, targetFile.length())

        val actualBytes = targetFile.readBytes()
        org.junit.Assert.assertArrayEquals(expectedNew, actualBytes)
    }

    @Test
    fun testBsPatchInvalidMagicThrows() {
        val oldFile = tempFolder.newFile("old_bad.bin")
        val targetFile = tempFolder.newFile("target_bad.bin")
        val patchFile = tempFolder.newFile("bad.patch")
        patchFile.writeBytes(ByteArray(64) { 0 }) // Bad magic

        try {
            BsPatch.applyPatch(oldFile, targetFile, patchFile)
            fail("Expected PatchException on bad magic")
        } catch (e: BsPatch.PatchException) {
            assertTrue(e.message?.contains("Unsupported patch format magic") == true)
        }
    }
}
