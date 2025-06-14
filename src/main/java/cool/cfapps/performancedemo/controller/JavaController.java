package cool.cfapps.performancedemo.controller;

import cool.cfapps.performancedemo.service.JavaBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/java")
public class JavaController {

    private final JavaBusinessService businessService;

    // Inject the proper executor for async operations
    @Autowired(required = false)
    @Qualifier("platformThreadExecutor")
    private Executor asyncExecutor;

    public JavaController(JavaBusinessService businessService) {
        this.businessService = businessService;
    }

    /**
     * Synchronous endpoint using platform threads
     */
    @GetMapping("/sync")
    public Map<String, Object> processSync(@RequestParam(defaultValue = "test") String input) {
        long startTime = System.currentTimeMillis();
        String result = businessService.processRequestSync(input);
        long endTime = System.currentTimeMillis();
        return Map.of(
                "result", result,
                "processingTime", endTime - startTime,
                "threadType", "Platform Thread",
                "threadName", Thread.currentThread().getName()
        );
    }

    /**
     * Asynchronous endpoint using CompletableFuture
     */
    @GetMapping("/async")
    public CompletableFuture<Map<String, Object>> processAsync(@RequestParam(defaultValue = "test") String input) {
        long startTime = System.currentTimeMillis();

        // Use dedicated executor instead of default ForkJoinPool.commonPool()
        Executor executor = (asyncExecutor != null) ? asyncExecutor :
                java.util.concurrent.ForkJoinPool.commonPool();

        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = businessService.processRequestSync(input); // Use sync method in async context
                long endTime = System.currentTimeMillis();

                return Map.of(
                        "result", result,
                        "processingTime", endTime - startTime,
                        "threadType", "Async with Custom Executor",
                        "threadName", Thread.currentThread().getName(),
                        "executor", executor.getClass().getSimpleName()
                );
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                return Map.of(
                        "error", e.getMessage(),
                        "processingTime", endTime - startTime,
                        "threadType", "Async Error",
                        "threadName", Thread.currentThread().getName()
                );
            }
        }, executor);
    }

    /**
     * Virtual threads endpoint
     * Run with profile: --spring.profiles.active=virtual-threads
     */
    @GetMapping("/virtual")
    public Map<String, Object> processVirtual(@RequestParam(defaultValue = "test") String input) {
        long startTime = System.currentTimeMillis();
        String result = businessService.processRequestVirtual(input);
        long endTime = System.currentTimeMillis();

        return Map.of(
                "result", result,
                "processingTime", endTime - startTime,
                "threadType", "Virtual Thread",
                "threadName", Thread.currentThread().getName(),
                "isVirtual", Thread.currentThread().isVirtual()
        );
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "Java Controller",
                "currentThread", Thread.currentThread().getName(),
                "isVirtual", Thread.currentThread().isVirtual()
        );
    }
}
