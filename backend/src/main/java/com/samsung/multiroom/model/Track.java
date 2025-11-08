package com.samsung.multiroom.model;

import javax.persistence.*;

@Entity
@Table(name = "tracks")
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    private String artist;
    
    private String album;
    
    @Column(length = 1000) // Allow longer file paths
    private String filePath; // Path to the audio file
    
    private String uri; // URI for streaming
    
    private int duration; // Duration in seconds
    
    private String albumArt; // Path to album art
    
    // Constructors
    public Track() {}
    
    public Track(String title, String artist, String album, String filePath, int duration) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
        this.duration = duration;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public String getAlbumArt() { return albumArt; }
    public void setAlbumArt(String albumArt) { this.albumArt = albumArt; }
}