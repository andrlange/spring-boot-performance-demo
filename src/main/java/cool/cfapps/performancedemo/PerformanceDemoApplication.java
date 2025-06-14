package cool.cfapps.performancedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {
        "cool.cfapps.performancedemo.controller",
        "cool.cfapps.performancedemo.service",
        "cool.cfapps.performancedemo"
})
public class PerformanceDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformanceDemoApplication.class, args);
    }
}

