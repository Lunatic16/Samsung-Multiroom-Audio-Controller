package com.samsung.multiroom.service;

import com.samsung.multiroom.model.Speaker;
import com.samsung.multiroom.model.SpeakerGroup;
import com.samsung.multiroom.model.Track;
import com.samsung.multiroom.repository.SpeakerGroupRepository;
import com.samsung.multiroom.repository.SpeakerRepository;
import com.samsung.multiroom.repository.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AudioPlaybackService {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioPlaybackService.class);
    
    @Autowired
    private SpeakerRepository speakerRepository;
    
    @Autowired
    private TrackRepository trackRepository;
    
    @Autowired
    private SpeakerGroupRepository speakerGroupRepository;
    
    // In-memory state of currently playing tracks and their playback position
    private final Map<String, TrackPlaybackState> playbackStates = new HashMap<>();
    
    // Simple class to hold playback state
    private static class TrackPlaybackState {
        private Track track;
        private int positionSeconds; // Current playback position in seconds
        private String status; // PLAYING, PAUSED, STOPPED
        private long startTime; // When playback started (for calculating position)
        
        public TrackPlaybackState(Track track) {
            this.track = track;
            this.positionSeconds = 0;
            this.status = "STOPPED";
            this.startTime = System.currentTimeMillis();
        }
        
        // Getters and setters
        public Track getTrack() { return track; }
        public void setTrack(Track track) { this.track = track; }
        
        public int getPositionSeconds() { 
            if (status.equals("PLAYING")) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                return Math.min(positionSeconds + (int)elapsed, track.getDuration());
            }
            return positionSeconds;
        }
        public void setPositionSeconds(int positionSeconds) { 
            this.positionSeconds = positionSeconds;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { 
            this.status = status;
            if (status.equals("PLAYING")) {
                this.startTime = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Play a track on a specific speaker
     */
    public boolean playTrackOnSpeaker(String speakerMacAddress, Long trackId) {
        logger.info("Playing track {} on speaker {}", trackId, speakerMacAddress);
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker == null) {
            logger.error("Speaker with MAC {} not found", speakerMacAddress);
            return false;
        }
        
        Track track = trackRepository.findById(trackId).orElse(null);
        if (track == null) {
            logger.error("Track with ID {} not found", trackId);
            return false;
        }
        
        // Update speaker state
        speaker.setCurrentTrack(track.getTitle());
        speaker.setStatus("PLAYING");
        speakerRepository.save(speaker);
        
        // Update playback state
        TrackPlaybackState state = new TrackPlaybackState(track);
        state.setStatus("PLAYING");
        playbackStates.put(speakerMacAddress, state);
        
        // Send command to speaker (in a real implementation, this would make HTTP requests to the speaker)
        sendPlayCommand(speaker, track.getUri());
        
        return true;
    }
    
    /**
     * Play a track on all speakers in a group
     */
    public boolean playTrackOnGroup(String groupName, Long trackId) {
        logger.info("Playing track {} on group {}", trackId, groupName);
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            logger.error("Speaker group {} not found", groupName);
            return false;
        }
        
        Track track = trackRepository.findById(trackId).orElse(null);
        if (track == null) {
            logger.error("Track with ID {} not found", trackId);
            return false;
        }
        
        // Update group state
        group.setPlaying(true);
        group.setCurrentTrackUri(track.getUri());
        speakerGroupRepository.save(group);
        
        // Update playback state and send command to each speaker in the group
        for (String speakerMac : group.getSpeakerMacAddresses()) {
            Speaker speaker = speakerRepository.findByMacAddress(speakerMac).orElse(null);
            if (speaker != null) {
                speaker.setCurrentTrack(track.getTitle());
                speaker.setStatus("PLAYING");
                speakerRepository.save(speaker);
                
                // Update playback state
                TrackPlaybackState state = new TrackPlaybackState(track);
                state.setStatus("PLAYING");
                playbackStates.put(speakerMac, state);
                
                // Send command to speaker
                sendPlayCommand(speaker, track.getUri());
            }
        }
        
        return true;
    }
    
    /**
     * Pause playback on a specific speaker
     */
    public boolean pauseSpeaker(String speakerMacAddress) {
        logger.info("Pausing playback on speaker {}", speakerMacAddress);
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker == null || speaker.getStatus().equals("STOPPED")) {
            return false;
        }
        
        speaker.setStatus("PAUSED");
        speakerRepository.save(speaker);
        
        TrackPlaybackState state = playbackStates.get(speakerMacAddress);
        if (state != null) {
            state.setPositionSeconds(state.getPositionSeconds());
            state.setStatus("PAUSED");
        }
        
        // Send pause command to speaker
        sendPauseCommand(speaker);
        
        return true;
    }
    
    /**
     * Pause playback on all speakers in a group
     */
    public boolean pauseGroup(String groupName) {
        logger.info("Pausing playback on group {}", groupName);
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            return false;
        }
        
        group.setPlaying(false);
        speakerGroupRepository.save(group);
        
        for (String speakerMac : group.getSpeakerMacAddresses()) {
            Speaker speaker = speakerRepository.findByMacAddress(speakerMac).orElse(null);
            if (speaker != null) {
                speaker.setStatus("PAUSED");
                speakerRepository.save(speaker);
                
                TrackPlaybackState state = playbackStates.get(speakerMac);
                if (state != null) {
                    state.setPositionSeconds(state.getPositionSeconds());
                    state.setStatus("PAUSED");
                }
                
                // Send pause command to speaker
                sendPauseCommand(speaker);
            }
        }
        
        return true;
    }
    
    /**
     * Resume playback on a specific speaker
     */
    public boolean resumeSpeaker(String speakerMacAddress) {
        logger.info("Resuming playback on speaker {}", speakerMacAddress);
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker == null || !speaker.getStatus().equals("PAUSED")) {
            return false;
        }
        
        speaker.setStatus("PLAYING");
        speakerRepository.save(speaker);
        
        TrackPlaybackState state = playbackStates.get(speakerMacAddress);
        if (state != null) {
            state.setStatus("PLAYING");
        }
        
        // Send resume command to speaker
        sendResumeCommand(speaker);
        
        return true;
    }
    
    /**
     * Stop playback on a specific speaker
     */
    public boolean stopSpeaker(String speakerMacAddress) {
        logger.info("Stopping playback on speaker {}", speakerMacAddress);
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker == null) {
            return false;
        }
        
        speaker.setStatus("STOPPED");
        speaker.setCurrentTrack(null);
        speakerRepository.save(speaker);
        
        playbackStates.remove(speakerMacAddress);
        
        // Send stop command to speaker
        sendStopCommand(speaker);
        
        return true;
    }
    
    /**
     * Set volume for a specific speaker
     */
    public boolean setVolume(String speakerMacAddress, int volume) {
        logger.info("Setting volume to {} for speaker {}", volume, speakerMacAddress);
        
        if (volume < 0 || volume > 100) {
            logger.error("Invalid volume value: {}. Volume must be between 0 and 100", volume);
            return false;
        }
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker != null) {
            speaker.setVolume(volume);
            speakerRepository.save(speaker);
            
            // Send volume command to speaker
            sendVolumeCommand(speaker, volume);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Set volume for all speakers in a group
     */
    public boolean setGroupVolume(String groupName, int volume) {
        logger.info("Setting volume to {} for group {}", volume, groupName);
        
        if (volume < 0 || volume > 100) {
            logger.error("Invalid volume value: {}", volume);
            return false;
        }
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            return false;
        }
        
        group.setVolume(volume);
        speakerGroupRepository.save(group);
        
        for (String speakerMac : group.getSpeakerMacAddresses()) {
            Speaker speaker = speakerRepository.findByMacAddress(speakerMac).orElse(null);
            if (speaker != null) {
                speaker.setVolume(volume);
                speakerRepository.save(speaker);
                
                // Send volume command to speaker
                sendVolumeCommand(speaker, volume);
            }
        }
        
        return true;
    }
    
    /**
     * Seek to position in track for a specific speaker
     */
    public boolean seekPosition(String speakerMacAddress, int seconds) {
        logger.info("Seeking to position {} seconds for speaker {}", seconds, speakerMacAddress);
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker == null) {
            return false;
        }
        
        TrackPlaybackState state = playbackStates.get(speakerMacAddress);
        if (state != null) {
            Track track = state.getTrack();
            if (track != null && seconds >= 0 && seconds <= track.getDuration()) {
                state.setPositionSeconds(seconds);
                
                // Send seek command to speaker
                sendSeekCommand(speaker, seconds);
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get current playback state for a speaker
     */
    public Map<String, Object> getPlaybackState(String speakerMacAddress) {
        Map<String, Object> state = new HashMap<>();
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker == null) {
            return null;
        }
        
        state.put("speakerId", speaker.getMacAddress());
        state.put("name", speaker.getName());
        state.put("status", speaker.getStatus());
        state.put("currentTrack", speaker.getCurrentTrack());
        state.put("volume", speaker.getVolume());
        
        TrackPlaybackState trackState = playbackStates.get(speakerMacAddress);
        if (trackState != null) {
            state.put("trackId", speaker.getCurrentTrack() != null ? trackState.getTrack().getId() : null);
            state.put("position", trackState.getPositionSeconds());
            state.put("duration", trackState.getTrack() != null ? trackState.getTrack().getDuration() : 0);
        } else {
            state.put("position", 0);
            state.put("duration", 0);
        }
        
        return state;
    }
    
    // Helper methods to send commands to speakers
    // In a real implementation, these would make HTTP requests to the actual speakers
    
    private void sendPlayCommand(Speaker speaker, String trackUri) {
        logger.info("Sending play command to speaker {} at IP {} with track URI: {}", 
                   speaker.getName(), speaker.getIpAddress(), trackUri);
        // In a real implementation: send HTTP request to speaker's IP:PORT to play the track
    }
    
    private void sendPauseCommand(Speaker speaker) {
        logger.info("Sending pause command to speaker {} at IP {}", speaker.getName(), speaker.getIpAddress());
        // In a real implementation: send HTTP request to speaker's IP:PORT to pause
    }
    
    private void sendResumeCommand(Speaker speaker) {
        logger.info("Sending resume command to speaker {} at IP {}", speaker.getName(), speaker.getIpAddress());
        // In a real implementation: send HTTP request to speaker's IP:PORT to resume
    }
    
    private void sendStopCommand(Speaker speaker) {
        logger.info("Sending stop command to speaker {} at IP {}", speaker.getName(), speaker.getIpAddress());
        // In a real implementation: send HTTP request to speaker's IP:PORT to stop
    }
    
    private void sendVolumeCommand(Speaker speaker, int volume) {
        logger.info("Sending volume command to speaker {} at IP {} with volume: {}", 
                   speaker.getName(), speaker.getIpAddress(), volume);
        // In a real implementation: send HTTP request to speaker's IP:PORT to set volume
    }
    
    private void sendSeekCommand(Speaker speaker, int seconds) {
        logger.info("Sending seek command to speaker {} at IP {} to position: {}", 
                   speaker.getName(), speaker.getIpAddress(), seconds);
        // In a real implementation: send HTTP request to speaker's IP:PORT to seek position
    }
}