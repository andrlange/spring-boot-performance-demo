package cool.cfapps.performancedemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Thread pool configuration for different threading models
 */
@Configuration
public class ThreadConfiguration {

    @Bean("platformThreadExecutor")
    @Profile("platform-threads")
    public Executor platformThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(500);
        executor.setMaxPoolSize(700);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("platform-");
        executor.initialize();
        return executor;
    }

    @Bean("virtualThreadExecutor")
    @Profile("virtual-threads")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
