package it.univ.lifeplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LifePlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifePlannerApplication.class, args);
    }
}
