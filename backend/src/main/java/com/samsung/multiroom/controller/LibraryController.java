package com.samsung.multiroom.controller;

import com.samsung.multiroom.config.MusicLibraryConfig;
import com.samsung.multiroom.model.Track;
import com.samsung.multiroom.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/library")
@CrossOrigin(origins = "*") // In production, specify your frontend URL
public class LibraryController {
    
    @Autowired
    private TrackRepository trackRepository;
    
    @Autowired
    private MusicLibraryConfig musicLibraryConfig;
    
    /**
     * Get all tracks in the library
     */
    @GetMapping("/tracks")
    public ResponseEntity<List<Track>> getAllTracks() {
        List<Track> tracks = trackRepository.findAll();
        return ResponseEntity.ok(tracks);
    }
    
    /**
     * Get a specific track by ID
     */
    @GetMapping("/tracks/{id}")
    public ResponseEntity<Track> getTrack(@PathVariable Long id) {
        return trackRepository.findById(id)
                .map(track -> ResponseEntity.ok(track))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Search tracks by title, artist, or album
     */
    @GetMapping("/search")
    public ResponseEntity<List<Track>> searchTracks(@RequestParam String query) {
        // In a real implementation, this would use a more sophisticated search
        // For now, we'll do a simple search on title
        List<Track> allTracks = trackRepository.findAll();
        List<Track> matchingTracks = allTracks.stream()
                .filter(track -> track.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                               (track.getArtist() != null && track.getArtist().toLowerCase().contains(query.toLowerCase())) ||
                               (track.getAlbum() != null && track.getAlbum().toLowerCase().contains(query.toLowerCase())))
                .toList();
        
        return ResponseEntity.ok(matchingTracks);
    }
    
    /**
     * Add a new track to the library
     */
    @PostMapping("/tracks")
    public ResponseEntity<Track> addTrack(@RequestBody Track track) {
        Track savedTrack = trackRepository.save(track);
        return ResponseEntity.ok(savedTrack);
    }
    
    /**
     * Update an existing track
     */
    @PutMapping("/tracks/{id}")
    public ResponseEntity<Track> updateTrack(@PathVariable Long id, @RequestBody Track track) {
        if (trackRepository.existsById(id)) {
            track.setId(id);
            Track updatedTrack = trackRepository.save(track);
            return ResponseEntity.ok(updatedTrack);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Delete a track from the library
     */
    @DeleteMapping("/tracks/{id}")
    public ResponseEntity<String> deleteTrack(@PathVariable Long id) {
        if (trackRepository.existsById(id)) {
            trackRepository.deleteById(id);
            return ResponseEntity.ok("Track deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get the current music library directory
     */
    @GetMapping("/config/directory")
    public ResponseEntity<String> getMusicLibraryDirectory() {
        return ResponseEntity.ok(musicLibraryConfig.getLibraryPath());
    }
    
    /**
     * Set the music library directory
     */
    @PostMapping("/config/directory")
    public ResponseEntity<String> setMusicLibraryDirectory(@RequestBody String directoryPath) {
        try {
            // Validate that the directory exists and is accessible
            Path path = Paths.get(directoryPath);
            
            // Create the directory if it doesn't exist
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            
            // Verify it's actually a directory
            if (!Files.isDirectory(path)) {
                return ResponseEntity.badRequest().body("Path is not a directory");
            }
            
            // Verify we can read from the directory
            if (!Files.isReadable(path)) {
                return ResponseEntity.badRequest().body("Directory is not readable");
            }
            
            // Update the configuration
            musicLibraryConfig.setLibraryPath(directoryPath);
            
            return ResponseEntity.ok("Music library directory set to: " + directoryPath);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error setting music library directory: " + e.getMessage());
        }
    }
    
    /**
     * Scan the music library directory and add all recognized audio files to the library
     */
    @PostMapping("/scan")
    public ResponseEntity<String> scanMusicLibrary() {
        try {
            String libraryPath = musicLibraryConfig.getLibraryPath();
            Path path = Paths.get(libraryPath);
            
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return ResponseEntity.badRequest().body("Music library directory does not exist or is not a directory");
            }
            
            // Get all supported audio files
            List<Path> audioFiles = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(this::isAudioFile)
                    .collect(Collectors.toList());
            
            int tracksAdded = 0;
            for (Path audioFile : audioFiles) {
                String fileName = audioFile.getFileName().toString();
                
                // Create a basic track entry if it doesn't already exist
                // In a real implementation, you'd extract metadata using a library like Apache Tika
                List<Track> existingTracks = trackRepository.findByFilePath(audioFile.toString());
                if (existingTracks.isEmpty()) {
                    Track track = new Track();
                    track.setTitle(extractTitleFromFilename(fileName));
                    track.setFilePath(audioFile.toString());
                    track.setUri("file://" + audioFile.toString()); // For local file access
                    
                    // These would normally be extracted from the file's metadata
                    track.setArtist("Unknown Artist");
                    track.setAlbum("Unknown Album");
                    track.setDuration(0); // Placeholder
                    
                    trackRepository.save(track);
                    tracksAdded++;
                }
            }
            
            return ResponseEntity.ok("Scanned directory. " + tracksAdded + " new tracks added to library.");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error scanning music library: " + e.getMessage());
        }
    }
    
    /**
     * Get list of all audio files found in the music library directory
     */
    @GetMapping("/files")
    public ResponseEntity<List<String>> getMusicFiles() {
        try {
            String libraryPath = musicLibraryConfig.getLibraryPath();
            Path path = Paths.get(libraryPath);
            
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return ResponseEntity.badRequest().body(List.of("Music library directory does not exist or is not a directory"));
            }
            
            List<String> audioFiles = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(this::isAudioFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(audioFiles);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(List.of("Error getting music files: " + e.getMessage()));
        }
    }
    
    /**
     * Check if a file has an audio extension
     */
    private boolean isAudioFile(Path path) {
        String fileName = path.toString().toLowerCase();
        return fileName.endsWith(".mp3") || 
               fileName.endsWith(".wav") || 
               fileName.endsWith(".flac") || 
               fileName.endsWith(".aac") || 
               fileName.endsWith(".ogg") || 
               fileName.endsWith(".m4a");
    }
    
    /**
     * Extract title from filename (basic implementation)
     */
    private String extractTitleFromFilename(String fileName) {
        // Remove file extension and replace underscores/dots with spaces
        String title = fileName.substring(0, fileName.lastIndexOf('.'));
        return title.replace('_', ' ').replace('.', ' ').trim();
    }
}