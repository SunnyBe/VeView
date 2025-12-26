package com.veview.veview_sdk.audiocapture

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A utility to write raw PCM audio data into a valid WAV file format.
 */
internal object WavFileUtil {

    /**
     * Writes a proper WAV header to the beginning of a file.
     *
     * @param file The file to write the header to.
     * @param sampleRate The sample rate of the audio.
     * @param numChannels The number of channels (1 for mono, 2 for stereo).
     * @param audioFormat The audio format (e.g., AudioFormat.ENCODING_PCM_16BIT).
     */
    fun writeWavHeader(file: File, sampleRate: Int, numChannels: Int, bitsPerSample: Int) {
        val randomAccessFile = RandomAccessFile(file, "rw")
        randomAccessFile.setLength(0) // Clear the file

        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            // RIFF chunk
            put('R'.code.toByte())
            put('I'.code.toByte())
            put('F'.code.toByte())
            put('F'.code.toByte())
            putInt(0) // Placeholder for chunk size
            put('W'.code.toByte())
            put('A'.code.toByte())
            put('V'.code.toByte())
            put('E'.code.toByte())

            // "fmt " sub-chunk
            put('f'.code.toByte())
            put('m'.code.toByte())
            put('t'.code.toByte())
            put(' '.code.toByte())
            putInt(16) // Sub-chunk 1 size (16 for PCM)
            putShort(1) // Audio format (1 for PCM)
            putShort(numChannels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())

            // "data" sub-chunk
            put('d'.code.toByte())
            put('a'.code.toByte())
            put('t'.code.toByte())
            put('a'.code.toByte())
            putInt(0) // Placeholder for data size
        }

        randomAccessFile.write(header.array())
        randomAccessFile.close()
    }

    /**
     * Updates the WAV header with the final file sizes. This should be called after all audio
     * data has been written.
     *
     * @param file The WAV file to update.
     */
    fun updateWavHeader(file: File) {
        val totalAudioLen = file.length() - 44
        val totalDataLen = totalAudioLen + 36

        val randomAccessFile = RandomAccessFile(file, "rw")
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)

        // Update ChunkSize (overall file size - 8)
        randomAccessFile.seek(4)
        randomAccessFile.write(buffer.putInt(0, totalDataLen.toInt()).array())

        // Update Subchunk2Size (audio data size)
        randomAccessFile.seek(40)
        randomAccessFile.write(buffer.putInt(0, totalAudioLen.toInt()).array())

        randomAccessFile.close()
    }
}