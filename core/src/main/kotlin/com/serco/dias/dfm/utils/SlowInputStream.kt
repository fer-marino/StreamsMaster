package com.serco.dias.dfm.utils


/**
 * Created by Fernando on 6/24/2016.
 */

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * InputStream wrapper to emulate a slow device
 *
 */
class SlowInputStream
/**
 * Wraps the input stream to emulate a slow device
 * @param `input` input stream
 * @param cps characters per second to emulate
 */
(input: InputStream, private val cps: Int) : FilterInputStream(input) {

    @Throws(IOException::class)
    override fun read(): Int {
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) { /* ignore */
        }

        return `in`.read()
    }

    // Also handles read(byte[])
    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val start = System.nanoTime()
        val out = `in`.read(b, off, len)

        val requiredTime = len * 1000 / cps
        if (System.nanoTime() - start > requiredTime * 1000000)
            try { // wait for the remaining time
                val diff = System.nanoTime() - start - requiredTime * 1000000
                val milli = diff / 1000000
                val nano = (diff % 1000000).toInt()
                Thread.sleep(milli, nano)
            } catch (e: InterruptedException) { /* ignore */
            }

        return out
    }

}