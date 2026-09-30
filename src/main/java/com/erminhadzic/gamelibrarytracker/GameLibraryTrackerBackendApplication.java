package com.erminhadzic.gamelibrarytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan("com.erminhadzic.gamelibrarytracker.model")
// Registers @ConfigurationProperties records such as JwtProperties as beans
@ConfigurationPropertiesScan
public class GameLibraryTrackerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameLibraryTrackerBackendApplication.class, args);
    }

}