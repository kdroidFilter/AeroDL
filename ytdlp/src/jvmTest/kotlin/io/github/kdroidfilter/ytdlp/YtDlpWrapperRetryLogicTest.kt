package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.Options
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YtDlpWrapperRetryLogicTest {

    @Test
    fun concurrentExtractorFailures_triggerSingleSelfUpdateExecution() = runBlocking {
        val wrapper = newTestWrapper()
        wrapper.downloadAvailabilityOverride = { true }
        wrapper.downloadNetworkCheckOverride = { _, _, _ -> Result.success(Unit) }

        val selfUpdateCalls = AtomicInteger(0)
        val attemptCountByUrl = ConcurrentHashMap<String, AtomicInteger>()
        val completed = CopyOnWriteArrayList<Boolean>()

        wrapper.selfUpdateRunnerOverride = {
            delay(80)
            selfUpdateCalls.incrementAndGet()
            true
        }

        wrapper.downloadAttemptRunnerOverride = { cmd, onEvent, emitStarted ->
            if (emitStarted) onEvent(Event.Started)
            val url = cmd.last()
            val callCount = attemptCountByUrl.computeIfAbsent(url) { AtomicInteger(0) }.incrementAndGet()

            if (callCount == 1) {
                YtDlpWrapper.DownloadAttemptResult(
                    exitCode = 1,
                    lines = listOf(
                        "WARNING: [youtube] test: nsig extraction failed: hash mismatch",
                        "ERROR: Signature extraction failed: Some formats may be missing",
                    ),
                )
            } else {
                YtDlpWrapper.DownloadAttemptResult(
                    exitCode = 0,
                    lines = listOf("[download] 100% of 1.00MiB at 1.00MiB/s"),
                )
            }
        }

        try {
            val options = Options(timeout = Duration.ofSeconds(5))
            val h1 = wrapper.download("https://www.youtube.com/watch?v=retryA", options) { event ->
                if (event is Event.Completed) completed.add(event.success)
            }
            val h2 = wrapper.download("https://www.youtube.com/watch?v=retryB", options) { event ->
                if (event is Event.Completed) completed.add(event.success)
            }

            withTimeout(5_000) { joinAll(h1.job, h2.job) }

            val totalAttempts = attemptCountByUrl.values.sumOf { it.get() }
            assertEquals(1, selfUpdateCalls.get(), "Self-update execution must be serialized and deduplicated.")
            assertEquals(3, totalAttempts, "Expected 2 initial attempts + 1 retry after update.")
            assertEquals(2, completed.size)
            assertEquals(1, completed.count { it })
            assertEquals(1, completed.count { !it })
        } finally {
            clearHooks(wrapper)
        }
    }

    @Test
    fun authenticationFailure_doesNotTriggerSelfUpdateOrRetry() = runBlocking {
        val wrapper = newTestWrapper()
        wrapper.downloadAvailabilityOverride = { true }
        wrapper.downloadNetworkCheckOverride = { _, _, _ -> Result.success(Unit) }

        val selfUpdateCalls = AtomicInteger(0)
        val attemptCalls = AtomicInteger(0)
        val events = CopyOnWriteArrayList<Event>()

        wrapper.selfUpdateRunnerOverride = {
            selfUpdateCalls.incrementAndGet()
            true
        }

        wrapper.downloadAttemptRunnerOverride = { _, onEvent, emitStarted ->
            attemptCalls.incrementAndGet()
            if (emitStarted) onEvent(Event.Started)
            YtDlpWrapper.DownloadAttemptResult(
                exitCode = 1,
                lines = listOf(
                    "ERROR: [youtube] auth: Sign in to confirm you're not a bot. Use --cookies-from-browser for the authentication.",
                ),
            )
        }

        try {
            val handle = wrapper.download(
                "https://www.youtube.com/watch?v=authOnly",
                Options(timeout = Duration.ofSeconds(5)),
            ) { event ->
                events.add(event)
            }

            withTimeout(5_000) { handle.job.join() }

            assertEquals(0, selfUpdateCalls.get(), "Authentication errors must not trigger self-update.")
            assertEquals(1, attemptCalls.get(), "Authentication errors must not trigger retry attempts.")
            assertTrue(events.filterIsInstance<Event.Log>().none { it.line.contains("retrying once", ignoreCase = true) })

            val completed = events.filterIsInstance<Event.Completed>().singleOrNull()
            assertNotNull(completed)
            assertFalse(completed.success)

            val error = events.filterIsInstance<Event.Error>().firstOrNull()
            assertNotNull(error)
            assertTrue(error.message.contains("Authentication required", ignoreCase = true))
        } finally {
            clearHooks(wrapper)
        }
    }

    private fun clearHooks(wrapper: YtDlpWrapper) {
        wrapper.downloadAvailabilityOverride = null
        wrapper.downloadNetworkCheckOverride = null
        wrapper.downloadAttemptRunnerOverride = null
        wrapper.selfUpdateRunnerOverride = null
        wrapper.nowProvider = { System.currentTimeMillis() }
    }

    private fun newTestWrapper(): YtDlpWrapper {
        runCatching { FileKit.init(appId = "aerodl-ytdlp-tests") }
        return YtDlpWrapper()
    }
}
