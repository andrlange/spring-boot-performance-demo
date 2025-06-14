package cool.cfapps.performancedemo.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Scope("singleton")
public class JavaBusinessService {

    private static final int MIN_DELAY_MS = 50;
    private static final int MAX_DELAY_MS = 200;


    // Counter for debugging (atomic for thread safety)
    private final java.util.concurrent.atomic.AtomicLong requestCounter = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * Thread-safe synchronous method
     */
    public String processRequestSync(String input) {
        long requestId = requestCounter.incrementAndGet();
        try {
            // Use ThreadLocalRandom (thread-safe)
            int delay = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MAX_DELAY_MS + 1);

            // Add some variance to avoid thundering herd
            if (requestId % 10 == 0) {
                delay += 10; // Every 10th request takes slightly longer
            }

            Thread.sleep(delay);

            return String.format("Java Sync [%d]: Processed '%s' in %dms on %s",
                    requestId, input, delay, Thread.currentThread().getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted for request " + requestId, e);
        }
    }

    /**
     * Thread-safe asynchronous method
     */
    public CompletableFuture<String> processRequestAsync(String input) {
        long requestId = requestCounter.incrementAndGet();

        return CompletableFuture.supplyAsync(() -> {
            try {
                int delay = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MAX_DELAY_MS + 1);
                Thread.sleep(delay);

                return String.format("Java Async [%d]: Processed '%s' in %dms on %s",
                        requestId, input, delay, Thread.currentThread().getName());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Async processing interrupted for request " + requestId, e);
            }
        });
    }

    /**
     * Thread-safe virtual thread method
     */
    public String processRequestVirtual(String input) {
        long requestId = requestCounter.incrementAndGet();

        try {
            int delay = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MAX_DELAY_MS + 1);
            Thread.sleep(delay);

            return String.format("Java Virtual [%d]: Processed '%s' in %dms on %s (virtual=%s)",
                    requestId, input, delay, Thread.currentThread().getName(), Thread.currentThread().isVirtual());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Virtual thread processing interrupted for request " + requestId, e);
        }
    }

    /**
     * Health check method
     */
    public String getServiceHealth() {
        return String.format("ThreadSafeJavaBusinessService: %d requests processed, running on %s",
                requestCounter.get(), Thread.currentThread().getName());
    }
}