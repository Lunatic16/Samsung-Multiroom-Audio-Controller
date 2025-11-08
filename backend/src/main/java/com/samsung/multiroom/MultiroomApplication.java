package com.samsung.multiroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.samsung.multiroom.config.MusicLibraryConfig;

@SpringBootApplication
@EnableConfigurationProperties(MusicLibraryConfig.class)
public class MultiroomApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiroomApplication.class, args);
    }
}