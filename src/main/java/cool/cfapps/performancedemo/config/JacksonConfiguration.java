package cool.cfapps.performancedemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration optimized for Kotlin support
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Configure proper ObjectMapper for Kotlin data classes
     * This improves serialization performance for Kotlin endpoints
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule.Builder().build());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
