package org.example.javaotellgtm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class JavaOtelLgtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaOtelLgtmApplication.class, args);
    }

}
