package com.samsung.multiroom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "music")
public class MusicLibraryConfig {
    
    private String libraryPath = "./music"; // Default path
    
    public String getLibraryPath() {
        return libraryPath;
    }
    
    public void setLibraryPath(String libraryPath) {
        this.libraryPath = libraryPath;
    }
}