package ru.softlogic.paylogic_kitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaylogicKitchenApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaylogicKitchenApplication.class, args);
    }

}
