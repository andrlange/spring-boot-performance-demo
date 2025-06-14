package cool.cfapps.performancedemo.service

import kotlinx.coroutines.delay
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Service
@Scope("singleton")  // Explicitly singleton, but thread-safe
class KotlinBusinessService {

    companion object {
        private const val MIN_DELAY_MS = 50L
        private const val MAX_DELAY_MS = 200L
    }

    // Thread-safe counter for debugging

    private val requestCounter = AtomicLong(0)

    /**
     * Thread-safe synchronous method
     */
    fun processRequestSync(input: String): String {
        val requestId = requestCounter.incrementAndGet()

        val delayMs = ThreadLocalRandom.current().nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1)

        // Add variance to avoid thundering herd
        val adjustedDelay = if (requestId % 10L == 0L) delayMs + 10 else delayMs

        Thread.sleep(adjustedDelay)

        return "Kotlin Sync [$requestId]: Processed '$input' in ${adjustedDelay}ms on ${Thread.currentThread().name}"
    }

    /**
     * Thread-safe coroutine-based method
     */
    suspend fun processRequestCoroutine(input: String): String {
        val requestId = requestCounter.incrementAndGet()

        // Use thread-safe random for coroutines
        val delayMs = Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1)

        // Add variance
        val adjustedDelay = if (requestId % 10L == 0L) delayMs + 10 else delayMs

        delay(adjustedDelay)

        return "Kotlin Coroutine [$requestId]: Processed '$input' in ${adjustedDelay}ms on ${Thread.currentThread().name}"
    }

    /**
     * Thread-safe virtual thread method
     */
    fun processRequestVirtual(input: String): String {
        val requestId = requestCounter.incrementAndGet()

        val delayMs = ThreadLocalRandom.current().nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1)

        // Add variance
        val adjustedDelay = if (requestId % 10L == 0L) delayMs + 10 else delayMs

        Thread.sleep(adjustedDelay)

        return "Kotlin Virtual [$requestId]: Processed '$input' in ${adjustedDelay}ms on ${Thread.currentThread().name} (virtual=${Thread.currentThread().isVirtual})"
    }

    /**
     * Health check method
     */
    fun getServiceHealth(): String {
        return "ThreadSafeKotlinBusinessService: ${requestCounter.get()} requests processed, running on ${Thread.currentThread().name}"
    }
}