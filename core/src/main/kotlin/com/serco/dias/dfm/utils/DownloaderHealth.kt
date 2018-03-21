package com.serco.dias.dfm.utils

import com.serco.dias.dfm.model.Center
import io.micrometer.core.instrument.MeterRegistry
import org.apache.commons.io.input.CountingInputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

@Component("Downloader")
class DownloaderHealth: AbstractHealthIndicator() {
    override fun doHealthCheck(builder: Health.Builder) {
        prevAggregateBandwidth = streamTable.map{ it.byteCount }.sum() - prevAggregateBandwidth
        builder.withDetail("bandwidth", prevAggregateBandwidth)
                .withDetail("active-downloads", streamTable.size)
                .withDetail("completed-downloads", completedDownloads)
                .withDetail("error-downloads", errorDownloads)
                .up()
    }

    private val streamTable = mutableSetOf<CountingInputStream>()

    private var prevAggregateBandwidth: Long = 0
    private var completedDownloads: Int = 0
    private var errorDownloads: Int = 0

    fun monitorDownload(stream: CountingInputStream, center: Center, runnable: () -> Unit) {
        try {
            streamTable.add(stream)
            runnable()
            completedDownloads++
        } catch (e: Exception) {
            errorDownloads++
            throw e
        } finally {
            streamTable.remove(stream)
        }
    }

}