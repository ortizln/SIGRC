package com.epmapa.sigrc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SigrcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SigrcApplication.class, args);
    }
}
