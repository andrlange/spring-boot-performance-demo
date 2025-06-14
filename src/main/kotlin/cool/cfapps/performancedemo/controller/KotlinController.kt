package cool.cfapps.performancedemo.controller

import cool.cfapps.performancedemo.service.KotlinBusinessService
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/kotlin")
class KotlinController(
    private val businessService: KotlinBusinessService
) {

    /**
     * Synchronous endpoint using platform threads
     * Test with: ab -n 100000 -c 100 http://localhost:8080/kotlin/sync?input=test
     */
    @GetMapping("/sync")
    fun processSync(@RequestParam(defaultValue = "test") input: String): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        val result = businessService.processRequestSync(input)
        val endTime = System.currentTimeMillis()

        return mapOf(
            "result" to result,
            "processingTime" to (endTime - startTime),
            "threadType" to "Platform Thread",
            "threadName" to Thread.currentThread().name
        )
    }

    /**
     * Coroutine endpoint using suspend functions
     * Test with: ab -n 100000 -c 100 http://localhost:8080/kotlin/coroutine?input=test
     */
    @GetMapping("/coroutine")
    fun processCoroutine(@RequestParam(defaultValue = "test") input: String): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        // Using runBlocking for simplicity in this demo
        // In production, you'd use proper coroutine context
        val result = runBlocking {
            businessService.processRequestCoroutine(input)
        }

        val endTime = System.currentTimeMillis()

        return mapOf(
            "result" to result,
            "processingTime" to (endTime - startTime),
            "threadType" to "Kotlin Coroutine",
            "threadName" to Thread.currentThread().name
        )
    }

    /**
     * Virtual threads endpoint
     * Test with: ab -n 100000 -c 100 http://localhost:8080/kotlin/virtual?input=test
     * Run with profile: --spring.profiles.active=virtual-threads
     */
    @GetMapping("/virtual")
    fun processVirtual(@RequestParam(defaultValue = "test") input: String): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        val result = businessService.processRequestVirtual(input)
        val endTime = System.currentTimeMillis()

        return mapOf(
            "result" to result,
            "processingTime" to (endTime - startTime),
            "threadType" to "Virtual Thread",
            "threadName" to Thread.currentThread().name,
            "isVirtual" to Thread.currentThread().isVirtual
        )
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "Kotlin Controller",
            "currentThread" to Thread.currentThread().name,
            "isVirtual" to Thread.currentThread().isVirtual
        )
    }
}