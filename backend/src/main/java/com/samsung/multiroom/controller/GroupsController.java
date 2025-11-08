package com.samsung.multiroom.controller;

import com.samsung.multiroom.model.SpeakerGroup;
import com.samsung.multiroom.service.SpeakerGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*") // In production, specify your frontend URL
public class GroupsController {
    
    @Autowired
    private SpeakerGroupService speakerGroupService;
    
    /**
     * Get all speaker groups
     */
    @GetMapping
    public ResponseEntity<List<SpeakerGroup>> getAllGroups() {
        List<SpeakerGroup> groups = speakerGroupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Get a specific group by name
     */
    @GetMapping("/{groupName}")
    public ResponseEntity<SpeakerGroup> getGroup(@PathVariable String groupName) {
        SpeakerGroup group = speakerGroupService.getGroup(groupName);
        if (group != null) {
            return ResponseEntity.ok(group);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Create a new speaker group
     */
    @PostMapping
    public ResponseEntity<SpeakerGroup> createGroup(@RequestBody Map<String, Object> request) {
        String groupName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        List<String> speakerMacAddresses = (List<String>) request.get("speakers");
        
        if (groupName == null || speakerMacAddresses == null) {
            return ResponseEntity.badRequest().build();
        }
        
        SpeakerGroup group = speakerGroupService.createGroup(groupName, speakerMacAddresses);
        if (group != null) {
            return ResponseEntity.ok(group);
        }
        return ResponseEntity.badRequest().build();
    }
    
    /**
     * Update an existing group
     */
    @PutMapping("/{groupName}")
    public ResponseEntity<SpeakerGroup> updateGroup(@PathVariable String groupName, 
                                                   @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> speakerMacAddresses = (List<String>) request.get("speakers");
        
        if (speakerMacAddresses == null) {
            return ResponseEntity.badRequest().build();
        }
        
        SpeakerGroup group = speakerGroupService.updateGroup(groupName, speakerMacAddresses);
        if (group != null) {
            return ResponseEntity.ok(group);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Delete a group
     */
    @DeleteMapping("/{groupName}")
    public ResponseEntity<String> deleteGroup(@PathVariable String groupName) {
        boolean deleted = speakerGroupService.deleteGroup(groupName);
        if (deleted) {
            return ResponseEntity.ok("Group deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Add a speaker to a group
     */
    @PostMapping("/{groupName}/speakers")
    public ResponseEntity<SpeakerGroup> addSpeakerToGroup(@PathVariable String groupName, 
                                                          @RequestBody Map<String, String> request) {
        String speakerMacAddress = request.get("speakerMac");
        
        if (speakerMacAddress == null) {
            return ResponseEntity.badRequest().build();
        }
        
        SpeakerGroup group = speakerGroupService.addSpeakerToGroup(groupName, speakerMacAddress);
        if (group != null) {
            return ResponseEntity.ok(group);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Remove a speaker from a group
     */
    @DeleteMapping("/{groupName}/speakers/{speakerMac}")
    public ResponseEntity<SpeakerGroup> removeSpeakerFromGroup(@PathVariable String groupName,
                                                               @PathVariable String speakerMac) {
        SpeakerGroup group = speakerGroupService.removeSpeakerFromGroup(groupName, speakerMac);
        if (group != null) {
            return ResponseEntity.ok(group);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get grouped speakers with their associations
     */
    @GetMapping("/organize")
    public ResponseEntity<Map<String, Object>> getGroupedSpeakers() {
        Map<String, Object> groupedSpeakers = speakerGroupService.getGroupedSpeakers();
        return ResponseEntity.ok(groupedSpeakers);
    }
    
    /**
     * Sync playback across all speakers in a group
     */
    @PostMapping("/{groupName}/sync")
    public ResponseEntity<String> syncGroupPlayback(@PathVariable String groupName) {
        boolean synced = speakerGroupService.syncGroupPlayback(groupName);
        if (synced) {
            return ResponseEntity.ok("Group playback synced successfully");
        }
        return ResponseEntity.notFound().build();
    }
}