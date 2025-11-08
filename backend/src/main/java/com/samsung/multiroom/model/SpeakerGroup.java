package com.samsung.multiroom.model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "speaker_groups")
public class SpeakerGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String name;
    
    @ElementCollection
    private List<String> speakerMacAddresses; // List of speaker MAC addresses in the group
    
    private boolean isPlaying;
    
    private String currentTrackUri;
    
    private int volume; // Group volume level
    
    // Constructors
    public SpeakerGroup() {}
    
    public SpeakerGroup(String name) {
        this.name = name;
        this.isPlaying = false;
        this.volume = 50; // Default volume
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<String> getSpeakerMacAddresses() { return speakerMacAddresses; }
    public void setSpeakerMacAddresses(List<String> speakerMacAddresses) { 
        this.speakerMacAddresses = speakerMacAddresses; 
    }
    
    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean playing) { isPlaying = playing; }
    
    public String getCurrentTrackUri() { return currentTrackUri; }
    public void setCurrentTrackUri(String currentTrackUri) { this.currentTrackUri = currentTrackUri; }
    
    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }
}