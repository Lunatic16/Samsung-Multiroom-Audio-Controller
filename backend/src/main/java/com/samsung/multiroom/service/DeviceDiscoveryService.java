package com.samsung.multiroom.service;

import com.samsung.multiroom.model.Speaker;
import com.samsung.multiroom.repository.SpeakerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DeviceDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceDiscoveryService.class);
    
    private static final int DISCOVERY_PORT = 1900; // UPnP discovery port
    private static final int SSDP_PORT = 1900;
    private static final String SSDP_ADDRESS = "239.255.255.250";
    private static final String DISCOVERY_MESSAGE = 
        "M-SEARCH * HTTP/1.1\r\n" +
        "HOST: 239.255.255.250:1900\r\n" +
        "MAN: \"ssdp:discover\"\r\n" +
        "MX: 3\r\n" +
        "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n";
    
    @Autowired
    private SpeakerRepository speakerRepository;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Store discovered devices
    private final List<Speaker> discoveredDevices = new ArrayList<>();
    
    @PostConstruct
    public void startDeviceDiscovery() {
        logger.info("Starting device discovery service...");
        // Start SSDP discovery in a separate thread
        CompletableFuture.runAsync(this::startSSDPDiscovery, executorService);
    }
    
    public List<Speaker> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
    
    /**
     * Performs network scan to find Samsung speakers
     */
    public void startSSDPDiscovery() {
        logger.info("Starting SSDP discovery for Samsung speakers...");
        
        try {
            // Create multicast socket
            InetAddress address = InetAddress.getByName(SSDP_ADDRESS);
            MulticastSocket socket = new MulticastSocket(SSDP_PORT);
            socket.setTimeToLive(4);
            socket.joinGroup(address);
            
            // Send discovery message
            DatagramPacket discoverPacket = new DatagramPacket(
                DISCOVERY_MESSAGE.getBytes(), 
                DISCOVERY_MESSAGE.length(), 
                address, 
                SSDP_PORT
            );
            
            socket.send(discoverPacket);
            
            // Listen for responses
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            
            // Listen for 5 seconds
            socket.setSoTimeout(5000);
            
            while (true) {
                try {
                    socket.receive(responsePacket);
                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    
                    if (response.contains("Samsung") || response.contains("Multiroom")) {
                        logger.info("Discovered Samsung device: {}", response);
                        processDiscoveryResponse(response, responsePacket.getAddress().getHostAddress());
                    }
                    
                    // Reset packet for next receive
                    responsePacket = new DatagramPacket(buffer, buffer.length);
                    
                } catch (SocketTimeoutException e) {
                    logger.info("Discovery timeout reached");
                    break;
                }
            }
            
            socket.leaveGroup(address);
            socket.close();
            
            logger.info("SSDP discovery completed. Found {} Samsung devices.", discoveredDevices.size());
            
        } catch (Exception e) {
            logger.error("Error during SSDP discovery: ", e);
        }
    }
    
    /**
     * Performs a simple port scan on the local network to find speaker devices
     */
    public void startNetworkScan() {
        logger.info("Starting network scan for speaker devices...");
        
        try {
            // Get local IP address to determine network range
            String localIP = getLocalIPAddress();
            if (localIP == null) {
                logger.error("Could not determine local IP address");
                return;
            }
            
            String networkPrefix = localIP.substring(0, localIP.lastIndexOf('.') + 1);
            
            // Scan IP range (e.g., 192.168.1.1 to 192.168.1.254)
            for (int i = 1; i < 255; i++) {
                String ip = networkPrefix + i;
                
                // Check if host is reachable
                if (isHostReachable(ip, 3000)) {
                    logger.debug("Host reachable: {}", ip);
                    // Attempt to identify if it's a speaker device
                    identifySpeakerDevice(ip);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during network scan: ", e);
        }
    }
    
    /**
     * Checks if a host is reachable
     */
    private boolean isHostReachable(String host, int timeout) {
        try {
            return InetAddress.getByName(host).isReachable(timeout);
        } catch (IOException e) {
            logger.debug("Host not reachable: {}", host);
            return false;
        }
    }
    
    /**
     * Attempts to identify speaker device at given IP
     */
    private void identifySpeakerDevice(String ipAddress) {
        // Check common ports used by Samsung speakers
        int[] commonPorts = {80, 8080, 49152, 49153, 49154};
        
        for (int port : commonPorts) {
            if (isPortOpen(ipAddress, port)) {
                logger.info("Found potential speaker device at {}:{}", ipAddress, port);
                
                // Look for existing device in DB or add new one
                Speaker existingSpeaker = speakerRepository.findByIpAddress(ipAddress).orElse(null);
                if (existingSpeaker == null) {
                    // Create new speaker device
                    Speaker newSpeaker = new Speaker("MAC_" + ipAddress.replace(".", ""), 
                                                   "Samsung Speaker " + ipAddress, ipAddress);
                    newSpeaker.setConnected(true);
                    newSpeaker.setModel("Samsung Multiroom Speaker");
                    speakerRepository.save(newSpeaker);
                    discoveredDevices.add(newSpeaker);
                } else {
                    // Update existing device
                    existingSpeaker.setConnected(true);
                    speakerRepository.save(existingSpeaker);
                    if (!discoveredDevices.contains(existingSpeaker)) {
                        discoveredDevices.add(existingSpeaker);
                    }
                }
                break; // Found device, move to next IP
            }
        }
    }
    
    /**
     * Checks if a port is open on the given host
     */
    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000); // 1 second timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Processes SSDP discovery response and creates a speaker object
     */
    private void processDiscoveryResponse(String response, String ipAddress) {
        // Parse the response to extract device information
        // This is a simplified version - in reality, you would parse the full SSDP response
        
        String macAddress = extractMacAddress(response, ipAddress);
        String deviceName = extractDeviceName(response);
        
        Speaker existingSpeaker = speakerRepository.findByMacAddress(macAddress).orElse(null);
        if (existingSpeaker == null) {
            Speaker newSpeaker = new Speaker(macAddress, deviceName, ipAddress);
            newSpeaker.setConnected(true);
            newSpeaker.setModel("Samsung Multiroom Speaker");
            speakerRepository.save(newSpeaker);
            discoveredDevices.add(newSpeaker);
        } else {
            existingSpeaker.setConnected(true);
            existingSpeaker.setIpAddress(ipAddress);
            speakerRepository.save(existingSpeaker);
            if (!discoveredDevices.contains(existingSpeaker)) {
                discoveredDevices.add(existingSpeaker);
            }
        }
    }
    
    /**
     * Extracts MAC address from response or generates one based on IP
     */
    private String extractMacAddress(String response, String ipAddress) {
        // In a real implementation, this would parse the actual MAC from the SSDP response
        // For now, generate one based on IP address
        return "MAC_" + ipAddress.replace(".", "");
    }
    
    /**
     * Extracts device name from response
     */
    private String extractDeviceName(String response) {
        // In a real implementation, this would parse the actual device name from the SSDP response
        return "Samsung Speaker";
    }
    
    /**
     * Returns the local IP address of the machine
     */
    private String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Error getting local IP address: ", e);
        }
        return null;
    }
    
    /**
     * Refreshes device discovery by scanning the network again
     */
    public void refreshDeviceDiscovery() {
        discoveredDevices.clear();
        CompletableFuture.runAsync(this::startSSDPDiscovery, executorService);
    }
}