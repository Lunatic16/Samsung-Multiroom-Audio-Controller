package com.samsung.multiroom.model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "speakers")
public class Speaker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String macAddress;
    
    private String name;
    
    private String ipAddress;
    
    private String model;
    
    private boolean connected;
    
    private boolean isMaster;
    
    private int volume;
    
    @ElementCollection
    private List<String> groupMembers;
    
    private String currentTrack;
    
    private String status; // PLAYING, PAUSED, STOPPED
    
    private int position; // Current playback position in seconds
    
    // Constructors
    public Speaker() {}
    
    public Speaker(String macAddress, String name, String ipAddress) {
        this.macAddress = macAddress;
        this.name = name;
        this.ipAddress = ipAddress;
        this.connected = false;
        this.volume = 50; // Default volume
        this.status = "STOPPED";
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    
    public boolean isMaster() { return isMaster; }
    public void setMaster(boolean master) { isMaster = master; }
    
    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }
    
    public List<String> getGroupMembers() { return groupMembers; }
    public void setGroupMembers(List<String> groupMembers) { this.groupMembers = groupMembers; }
    
    public String getCurrentTrack() { return currentTrack; }
    public void setCurrentTrack(String currentTrack) { this.currentTrack = currentTrack; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}