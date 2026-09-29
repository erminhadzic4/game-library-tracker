package com.erminhadzic.gamelibrarytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan("com.erminhadzic.gamelibrarytracker.model")
public class GameLibraryTrackerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameLibraryTrackerBackendApplication.class, args);
    }

}