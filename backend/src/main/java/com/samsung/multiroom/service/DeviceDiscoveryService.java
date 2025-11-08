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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n" +
        "USER-AGENT: Linux/1.0 UPnP/1.1 multiroom-audio/1.0\r\n\r\n";
    
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
            socket.setSoTimeout(5000); // 5 second timeout
            
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
            byte[] buffer = new byte[8192]; // Larger buffer to accommodate full responses
            
            long startTime = System.currentTimeMillis();
            long timeout = 6000; // 6 seconds total timeout
            
            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(responsePacket);
                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength(), "UTF-8");
                    
                    String sourceIp = responsePacket.getAddress().getHostAddress();
                    
                    if (response.contains("Samsung") || response.contains("Multiroom") || 
                        response.contains("AVTransport") || response.contains("MediaRenderer") || 
                        response.toLowerCase().contains("samsung")) {
                        
                        logger.info("Discovered Samsung device at {}: {}", sourceIp, response);
                        processDiscoveryResponse(response, sourceIp);
                    }
                    
                } catch (SocketTimeoutException e) {
                    logger.info("Discovery timeout reached");
                    break;
                } catch (Exception e) {
                    logger.warn("Error processing discovery response: ", e);
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
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 1; i < 255; i++) {
                final String ip = networkPrefix + i;
                
                // Process each IP asynchronously to speed up discovery
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    if (isHostReachable(ip, 1000)) { // Lower timeout for faster scanning
                        logger.debug("Host reachable: {}", ip);
                        // Attempt to identify if it's a speaker device
                        identifySpeakerDevice(ip);
                    }
                }, executorService);
                
                futures.add(future);
                
                // Limit the number of concurrent connections to prevent overwhelming network
                if (futures.size() >= 10) {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    futures.clear();
                }
            }
            
            // Wait for any remaining futures to complete
            if (!futures.isEmpty()) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
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
            // Use a more reliable ping method
            ProcessBuilder processBuilder = new ProcessBuilder("ping", "-c", "1", "-W", String.valueOf(timeout/1000), host);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.debug("Host not reachable via ping: {} ({})", host, e.getMessage());
        }
        
        // Fallback to Java's isReachable method
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
        // Check common ports used by Samsung speakers and similar devices
        int[] commonPorts = {80, 8080, 49152, 49153, 49154, 8008, 9000, 1900};
        
        for (int port : commonPorts) {
            if (isPortOpen(ipAddress, port)) {
                logger.info("Found potential speaker device at {}:{}", ipAddress, port);
                
                // Look for existing device in DB or add new one
                Speaker existingSpeaker = speakerRepository.findByIpAddress(ipAddress).orElse(null);
                if (existingSpeaker == null) {
                    // Create new speaker device
                    String macAddress = generateMacAddress(ipAddress);
                    Speaker newSpeaker = new Speaker(macAddress, 
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
                // Continue checking other ports as there might be multiple services running on different ports
            }
        }
    }
    
    /**
     * Checks if a port is open on the given host
     */
    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            // Use a shorter timeout for faster scanning
            socket.connect(new InetSocketAddress(host, port), 500); // 500ms timeout
            return true;
        } catch (IOException e) {
            return false; // Port is closed or unreachable
        }
    }
    
    /**
     * Processes SSDP discovery response and creates a speaker object
     */
    private void processDiscoveryResponse(String response, String ipAddress) {
        // More robust parsing of SSDP response to extract device information
        String usn = extractHeader(response, "USN");
        String location = extractHeader(response, "LOCATION");
        String server = extractHeader(response, "SERVER");
        String st = extractHeader(response, "ST");
        
        logger.debug("Processing SSDP response from {}: USN={}, LOCATION={}, SERVER={}, ST={}", 
                    ipAddress, usn, location, server, st);
        
        // Generate a MAC address based on the IP address if not available in USN
        String macAddress = "MAC_" + ipAddress.replace(".", "");
        if (usn != null && usn.toLowerCase().contains("mac")) {
            // Try to extract actual MAC from USN if present
            String extractedMac = extractMacFromUsn(usn);
            if (extractedMac != null) {
                macAddress = extractedMac;
            }
        }
        
        String deviceName = "Samsung Speaker " + ipAddress;
        if (server != null && !server.isEmpty()) {
            deviceName = server.split(" ")[0] + " " + ipAddress;
        }
        
        Speaker existingSpeaker = speakerRepository.findByMacAddress(macAddress).orElse(null);
        if (existingSpeaker == null) {
            Speaker newSpeaker = new Speaker(macAddress, deviceName, ipAddress);
            newSpeaker.setConnected(true);
            newSpeaker.setModel(extractModelFromResponse(response));
            
            // Try to get additional details from device if possible
            // For now, just save the info we have
            speakerRepository.save(newSpeaker);
            discoveredDevices.add(newSpeaker);
        } else {
            existingSpeaker.setConnected(true);
            existingSpeaker.setIpAddress(ipAddress);
            existingSpeaker.setModel(extractModelFromResponse(response));
            speakerRepository.save(existingSpeaker);
            if (!discoveredDevices.contains(existingSpeaker)) {
                discoveredDevices.add(existingSpeaker);
            }
        }
    }
    
    /**
     * Extracts a header value from the SSDP response
     */
    private String extractHeader(String response, String headerName) {
        String[] lines = response.split("\r\n");
        String searchPattern = headerName + ":";
        
        for (String line : lines) {
            if (line.toLowerCase().startsWith(headerName.toLowerCase() + ":")) {
                return line.substring(searchPattern.length()).trim();
            }
        }
        
        return null;
    }
    
    /**
     * Extracts Model information from the response
     */
    private String extractModelFromResponse(String response) {
        String model = extractHeader(response, "SERVER");
        if (model == null) {
            model = extractHeader(response, "USER-AGENT");
        }
        return model != null ? model : "Samsung Multiroom Speaker";
    }
    
    /**
     * Extracts MAC address from USN if present
     */
    private String extractMacFromUsn(String usn) {
        // Common MAC address pattern
        Pattern macPattern = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
        Matcher matcher = macPattern.matcher(usn);
        
        if (matcher.find()) {
            return matcher.group(0);
        }
        
        return null;
    }
    
    /**
     * Generates a consistent MAC address based on IP address
     */
    private String generateMacAddress(String ipAddress) {
        // Convert IP to a pseudo-MAC format
        String ipNumbers = ipAddress.replaceAll("\\.", "");
        return "MAC_" + ipNumbers;
    }
    
    /**
     * Returns the local IP address of the machine
     */
    private String getLocalIPAddress() {
        try {
            // Get the default route and its associated interface
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); 
                 interfaces.hasMoreElements();) {
                
                NetworkInterface networkInterface = interfaces.nextElement();
                
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || 
                    !networkInterface.isUp() || networkInterface.isPointToPoint()) {
                    continue;
                }
                
                for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); 
                     addresses.hasMoreElements();) {
                    
                    InetAddress addr = addresses.nextElement();
                    
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && 
                        !addr.isAnyLocalAddress() && isPrivateAddress(addr)) {
                        
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
     * Checks if an address is private
     */
    private boolean isPrivateAddress(InetAddress addr) {
        return addr.isSiteLocalAddress() && 
               !addr.getHostAddress().startsWith("169.254"); // Exclude link-local
    }
    
    /**
     * Refreshes device discovery by scanning the network again
     */
    public void refreshDeviceDiscovery() {
        logger.info("Refreshing device discovery...");
        discoveredDevices.clear();
        CompletableFuture.runAsync(this::startSSDPDiscovery, executorService);
        CompletableFuture.runAsync(this::startNetworkScan, executorService);
    }
    
    /**
     * Refreshes device discovery by scanning the network again
     */
    public void startFullDiscovery() {
        logger.info("Starting full device discovery (both SSDP and network scan)...");
        discoveredDevices.clear();
        CompletableFuture.runAsync(this::startSSDPDiscovery, executorService);
        CompletableFuture.runAsync(this::startNetworkScan, executorService);
    }
    
    /**
     * Check the status of an existing speaker
     */
    public boolean checkSpeakerStatus(String ipAddress) {
        if (ipAddress == null) return false;
        
        // Quick test to see if device is responding
        boolean isReachable = isHostReachable(ipAddress, 1000);
        
        // Update the speaker's status in the repository and list
        Speaker speaker = speakerRepository.findByIpAddress(ipAddress).orElse(null);
        if (speaker != null) {
            speaker.setConnected(isReachable);
            speakerRepository.save(speaker);
            
            // Update the discovered devices list
            for (int i = 0; i < discoveredDevices.size(); i++) {
                if (discoveredDevices.get(i).getIpAddress().equals(ipAddress)) {
                    discoveredDevices.get(i).setConnected(isReachable);
                    break;
                }
            }
        }
        
        return isReachable;
    }
}