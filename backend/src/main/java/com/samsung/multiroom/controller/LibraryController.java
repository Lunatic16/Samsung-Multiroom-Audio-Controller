package com.samsung.multiroom.controller;

import com.samsung.multiroom.model.Track;
import com.samsung.multiroom.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@CrossOrigin(origins = "*") // In production, specify your frontend URL
public class LibraryController {
    
    @Autowired
    private TrackRepository trackRepository;
    
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
}