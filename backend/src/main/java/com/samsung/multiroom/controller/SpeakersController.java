package com.samsung.multiroom.controller;

import com.samsung.multiroom.model.Speaker;
import com.samsung.multiroom.service.DeviceDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/speakers")
@CrossOrigin(origins = "*") // In production, specify your frontend URL
public class SpeakersController {
    
    @Autowired
    private DeviceDiscoveryService deviceDiscoveryService;
    
    /**
     * Get all discovered speakers
     */
    @GetMapping
    public ResponseEntity<List<Speaker>> getAllSpeakers() {
        return ResponseEntity.ok(deviceDiscoveryService.getDiscoveredDevices());
    }
    
    /**
     * Refresh device discovery
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshDevices() {
        deviceDiscoveryService.refreshDeviceDiscovery();
        return ResponseEntity.ok("Device discovery refreshed");
    }
    
    /**
     * Get a specific speaker by MAC address
     */
    @GetMapping("/{macAddress}")
    public ResponseEntity<Speaker> getSpeaker(@PathVariable String macAddress) {
        // In a real implementation, this would query the database
        // For now, we'll return from the discovered devices list
        List<Speaker> speakers = deviceDiscoveryService.getDiscoveredDevices();
        for (Speaker speaker : speakers) {
            if (speaker.getMacAddress().equals(macAddress)) {
                return ResponseEntity.ok(speaker);
            }
        }
        return ResponseEntity.notFound().build();
    }
}