package com.samsung.multiroom.controller;

import com.samsung.multiroom.service.AudioPlaybackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playback")
@CrossOrigin(origins = "*") // In production, specify your frontend URL
public class PlaybackController {
    
    @Autowired
    private AudioPlaybackService audioPlaybackService;
    
    /**
     * Play a track on a specific speaker
     */
    @PostMapping("/speakers/{speakerMac}/play")
    public ResponseEntity<String> playTrackOnSpeaker(@PathVariable String speakerMac, 
                                                     @RequestBody Map<String, Long> request) {
        Long trackId = request.get("trackId");
        
        if (trackId == null) {
            return ResponseEntity.badRequest().body("Track ID is required");
        }
        
        boolean success = audioPlaybackService.playTrackOnSpeaker(speakerMac, trackId);
        if (success) {
            return ResponseEntity.ok("Track started on speaker");
        }
        return ResponseEntity.badRequest().body("Failed to start track on speaker");
    }
    
    /**
     * Play a track on a speaker group
     */
    @PostMapping("/groups/{groupName}/play")
    public ResponseEntity<String> playTrackOnGroup(@PathVariable String groupName, 
                                                   @RequestBody Map<String, Long> request) {
        Long trackId = request.get("trackId");
        
        if (trackId == null) {
            return ResponseEntity.badRequest().body("Track ID is required");
        }
        
        boolean success = audioPlaybackService.playTrackOnGroup(groupName, trackId);
        if (success) {
            return ResponseEntity.ok("Track started on group");
        }
        return ResponseEntity.badRequest().body("Failed to start track on group");
    }
    
    /**
     * Pause playback on a specific speaker
     */
    @PostMapping("/speakers/{speakerMac}/pause")
    public ResponseEntity<String> pauseSpeaker(@PathVariable String speakerMac) {
        boolean success = audioPlaybackService.pauseSpeaker(speakerMac);
        if (success) {
            return ResponseEntity.ok("Playback paused on speaker");
        }
        return ResponseEntity.badRequest().body("Failed to pause speaker");
    }
    
    /**
     * Pause playback on a speaker group
     */
    @PostMapping("/groups/{groupName}/pause")
    public ResponseEntity<String> pauseGroup(@PathVariable String groupName) {
        boolean success = audioPlaybackService.pauseGroup(groupName);
        if (success) {
            return ResponseEntity.ok("Playback paused on group");
        }
        return ResponseEntity.badRequest().body("Failed to pause group");
    }
    
    /**
     * Resume playback on a specific speaker
     */
    @PostMapping("/speakers/{speakerMac}/resume")
    public ResponseEntity<String> resumeSpeaker(@PathVariable String speakerMac) {
        boolean success = audioPlaybackService.resumeSpeaker(speakerMac);
        if (success) {
            return ResponseEntity.ok("Playback resumed on speaker");
        }
        return ResponseEntity.badRequest().body("Failed to resume speaker");
    }
    
    /**
     * Stop playback on a specific speaker
     */
    @PostMapping("/speakers/{speakerMac}/stop")
    public ResponseEntity<String> stopSpeaker(@PathVariable String speakerMac) {
        boolean success = audioPlaybackService.stopSpeaker(speakerMac);
        if (success) {
            return ResponseEntity.ok("Playback stopped on speaker");
        }
        return ResponseEntity.badRequest().body("Failed to stop speaker");
    }
    
    /**
     * Set volume for a specific speaker
     */
    @PutMapping("/speakers/{speakerMac}/volume")
    public ResponseEntity<String> setSpeakerVolume(@PathVariable String speakerMac, 
                                                  @RequestBody Map<String, Integer> request) {
        Integer volume = request.get("volume");
        
        if (volume == null) {
            return ResponseEntity.badRequest().body("Volume is required");
        }
        
        boolean success = audioPlaybackService.setVolume(speakerMac, volume);
        if (success) {
            return ResponseEntity.ok("Volume set successfully");
        }
        return ResponseEntity.badRequest().body("Failed to set volume");
    }
    
    /**
     * Set volume for a speaker group
     */
    @PutMapping("/groups/{groupName}/volume")
    public ResponseEntity<String> setGroupVolume(@PathVariable String groupName, 
                                                @RequestBody Map<String, Integer> request) {
        Integer volume = request.get("volume");
        
        if (volume == null) {
            return ResponseEntity.badRequest().body("Volume is required");
        }
        
        boolean success = audioPlaybackService.setGroupVolume(groupName, volume);
        if (success) {
            return ResponseEntity.ok("Group volume set successfully");
        }
        return ResponseEntity.badRequest().body("Failed to set group volume");
    }
    
    /**
     * Seek to a position in the current track for a specific speaker
     */
    @PutMapping("/speakers/{speakerMac}/seek")
    public ResponseEntity<String> seekPosition(@PathVariable String speakerMac, 
                                              @RequestBody Map<String, Integer> request) {
        Integer seconds = request.get("seconds");
        
        if (seconds == null) {
            return ResponseEntity.badRequest().body("Seconds is required");
        }
        
        boolean success = audioPlaybackService.seekPosition(speakerMac, seconds);
        if (success) {
            return ResponseEntity.ok("Position seeked successfully");
        }
        return ResponseEntity.badRequest().body("Failed to seek position");
    }
    
    /**
     * Get current playback state for a specific speaker
     */
    @GetMapping("/speakers/{speakerMac}/state")
    public ResponseEntity<Map<String, Object>> getPlaybackState(@PathVariable String speakerMac) {
        Map<String, Object> state = audioPlaybackService.getPlaybackState(speakerMac);
        if (state != null) {
            return ResponseEntity.ok(state);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get playback states for all speakers in a group
     */
    @GetMapping("/groups/{groupName}/states")
    public ResponseEntity<List<Map<String, Object>>> getGroupPlaybackStates(@PathVariable String groupName) {
        // This would require a new method in the service to get states for all speakers in a group
        // For now, we'll return a message indicating this would be implemented
        return ResponseEntity.ok(List.of());
    }
}