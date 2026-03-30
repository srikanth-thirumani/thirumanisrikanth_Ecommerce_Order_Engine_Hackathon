package com.ecommerce;

import com.ecommerce.cli.CLIMenuRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EcommerceOrderEngineApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(EcommerceOrderEngineApplication.class, args);
        CLIMenuRunner runner = context.getBean(CLIMenuRunner.class);
        runner.run();
    }
}
