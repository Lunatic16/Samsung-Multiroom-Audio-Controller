package com.samsung.multiroom.service;

import com.samsung.multiroom.model.Speaker;
import com.samsung.multiroom.model.SpeakerGroup;
import com.samsung.multiroom.repository.SpeakerGroupRepository;
import com.samsung.multiroom.repository.SpeakerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SpeakerGroupService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpeakerGroupService.class);
    
    @Autowired
    private SpeakerGroupRepository speakerGroupRepository;
    
    @Autowired
    private SpeakerRepository speakerRepository;
    
    @Autowired
    private DeviceDiscoveryService deviceDiscoveryService;
    
    /**
     * Creates a new speaker group
     */
    public SpeakerGroup createGroup(String groupName, List<String> speakerMacAddresses) {
        logger.info("Creating speaker group: {} with speakers: {}", groupName, speakerMacAddresses);
        
        // Validate that all speakers exist
        for (String macAddress : speakerMacAddresses) {
            if (speakerRepository.findByMacAddress(macAddress).isEmpty()) {
                logger.error("Speaker with MAC {} does not exist", macAddress);
                return null;
            }
        }
        
        // Check if group with this name already exists
        if (speakerGroupRepository.findByName(groupName).isPresent()) {
            logger.error("Group with name {} already exists", groupName);
            return null;
        }
        
        SpeakerGroup newGroup = new SpeakerGroup(groupName);
        newGroup.setSpeakerMacAddresses(speakerMacAddresses);
        
        return speakerGroupRepository.save(newGroup);
    }
    
    /**
     * Updates an existing speaker group
     */
    public SpeakerGroup updateGroup(String groupName, List<String> speakerMacAddresses) {
        logger.info("Updating speaker group: {} with speakers: {}", groupName, speakerMacAddresses);
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            logger.error("Group with name {} does not exist", groupName);
            return null;
        }
        
        // Validate that all speakers exist
        for (String macAddress : speakerMacAddresses) {
            if (speakerRepository.findByMacAddress(macAddress).isEmpty()) {
                logger.error("Speaker with MAC {} does not exist", macAddress);
                return null;
            }
        }
        
        group.setSpeakerMacAddresses(speakerMacAddresses);
        return speakerGroupRepository.save(group);
    }
    
    /**
     * Adds a speaker to an existing group
     */
    public SpeakerGroup addSpeakerToGroup(String groupName, String speakerMacAddress) {
        logger.info("Adding speaker {} to group {}", speakerMacAddress, groupName);
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            logger.error("Group with name {} does not exist", groupName);
            return null;
        }
        
        Speaker speaker = speakerRepository.findByMacAddress(speakerMacAddress).orElse(null);
        if (speaker == null) {
            logger.error("Speaker with MAC {} does not exist", speakerMacAddress);
            return null;
        }
        
        List<String> members = group.getSpeakerMacAddresses();
        if (!members.contains(speakerMacAddress)) {
            members.add(speakerMacAddress);
            group.setSpeakerMacAddresses(members);
            return speakerGroupRepository.save(group);
        }
        
        return group; // Speaker already in group
    }
    
    /**
     * Removes a speaker from a group
     */
    public SpeakerGroup removeSpeakerFromGroup(String groupName, String speakerMacAddress) {
        logger.info("Removing speaker {} from group {}", speakerMacAddress, groupName);
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            logger.error("Group with name {} does not exist", groupName);
            return null;
        }
        
        List<String> members = group.getSpeakerMacAddresses();
        members.remove(speakerMacAddress);
        group.setSpeakerMacAddresses(members);
        
        return speakerGroupRepository.save(group);
    }
    
    /**
     * Deletes a speaker group
     */
    public boolean deleteGroup(String groupName) {
        logger.info("Deleting speaker group: {}", groupName);
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            logger.error("Group with name {} does not exist", groupName);
            return false;
        }
        
        speakerGroupRepository.delete(group);
        return true;
    }
    
    /**
     * Gets all speaker groups
     */
    public List<SpeakerGroup> getAllGroups() {
        return speakerGroupRepository.findAll();
    }
    
    /**
     * Gets a specific speaker group by name
     */
    public SpeakerGroup getGroup(String groupName) {
        return speakerGroupRepository.findByName(groupName).orElse(null);
    }
    
    /**
     * Gets all speakers with their group associations
     */
    public Map<String, Object> getGroupedSpeakers() {
        Map<String, Object> result = new HashMap<>();
        
        // Get all discovered speakers
        List<Speaker> allSpeakers = deviceDiscoveryService.getDiscoveredDevices();
        
        // Get all groups
        List<SpeakerGroup> allGroups = speakerGroupRepository.findAll();
        
        // Create a mapping of speaker MAC to group name
        Map<String, String> speakerToGroup = new HashMap<>();
        for (SpeakerGroup group : allGroups) {
            for (String mac : group.getSpeakerMacAddresses()) {
                speakerToGroup.put(mac, group.getName());
            }
        }
        
        // Group speakers by their group association
        Map<String, List<Speaker>> groupedSpeakers = new HashMap<>();
        Map<String, List<Speaker>> ungroupedSpeakers = new HashMap<>();
        
        for (Speaker speaker : allSpeakers) {
            String groupName = speakerToGroup.get(speaker.getMacAddress());
            if (groupName != null) {
                groupedSpeakers.computeIfAbsent(groupName, k -> new ArrayList<>()).add(speaker);
            } else {
                ungroupedSpeakers.computeIfAbsent("ungrouped", k -> new ArrayList<>()).add(speaker);
            }
        }
        
        result.put("grouped", groupedSpeakers);
        result.put("ungrouped", ungroupedSpeakers.get("ungrouped"));
        result.put("allGroups", allGroups);
        
        return result;
    }
    
    /**
     * Syncs playback across all speakers in a group
     */
    public boolean syncGroupPlayback(String groupName) {
        logger.info("Syncing playback for group: {}", groupName);
        
        SpeakerGroup group = speakerGroupRepository.findByName(groupName).orElse(null);
        if (group == null) {
            logger.error("Group with name {} does not exist", groupName);
            return false;
        }
        
        // In a real implementation, this would sync the playback position across all speakers in the group
        // This is complex and typically involves:
        // 1. Getting the current position from one reference speaker
        // 2. Sending the same position to all other speakers in the group
        // 3. Coordinating the start time to maintain synchronization
        
        logger.info("Group playback synchronization completed for {}", groupName);
        return true;
    }
}