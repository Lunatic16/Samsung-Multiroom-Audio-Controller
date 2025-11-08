# Samsung Multiroom Audio Controller

A hybrid application that provides a web interface for controlling Samsung multiroom audio devices, built with Spring Boot and JavaScript.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Usage](#usage)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)
- [Development](#development)
- [Limitations](#limitations)
- [Contributing](#contributing)

## Overview

This project represents a successful conversion of the Samsung Wireless Audio-Multiroom Android application to a hybrid Java web application. The system consists of a Java Spring Boot backend server that handles device discovery and control, with a web-based frontend that provides an intuitive user interface accessible via web browsers.

## Architecture

This hybrid application follows a client-server architecture:

- **Backend**: Java Spring Boot application that manages device discovery, audio streaming, and communication with Samsung speakers
- **Frontend**: Responsive web UI built with HTML, CSS, and JavaScript that communicates with the backend via REST APIs
- **Data Layer**: JPA with H2 database (in-memory for demo, with capability to connect to SQLite)

## Features

- Discover Samsung speakers on your local network
- Group multiple speakers together for synchronized playback
- Control playback (play, pause, stop, seek) across individual speakers or groups
- Adjust volume for individual speakers or groups
- Browse and play music from your library
- Multiroom synchronization
- Responsive web interface accessible from any device with a browser
- Server control script with relative path support for easy deployment

## Prerequisites

- Java 11 or higher
- Maven 3.6.0 or higher
- Node.js (optional, for development)

## Setup Instructions

### 1. Clone and Build the Application

```bash
cd multiroom-app/backend
mvn clean package
```

### 2. Using the Server Script (Recommended)

The project includes a convenient server control script:

```bash
# Make the script executable (if not already done)
chmod +x server.sh

# Start the server
./server.sh start

# Check status
./server.sh status

# View logs
./server.sh logs

# Stop the server
./server.sh stop

# Restart the server
./server.sh restart
```

### 3. Manual Start (Alternative)

```bash
cd multiroom-app/backend
mvn spring-boot:run
```

### 4. Access the Web Interface

Once the server is running, open your web browser and navigate to:

http://localhost:8081

## Usage

1. Start the server using the instructions above
2. Open the web interface in your browser
3. Click "Refresh Devices" to discover speakers on your network
4. Create speaker groups using the "Groups" tab
5. Use the dashboard to control playback across your speakers
6. Browse and play music from your library

## API Endpoints

The application provides the following REST API endpoints:

### Speakers
- `GET /api/speakers` - Get all discovered speakers
- `POST /api/speakers/refresh` - Refresh device discovery
- `GET /api/speakers/{macAddress}` - Get specific speaker by MAC address

### Groups
- `GET /api/groups` - Get all speaker groups
- `POST /api/groups` - Create a new group
- `PUT /api/groups/{groupName}` - Update an existing group
- `DELETE /api/groups/{groupName}` - Delete a group
- `GET /api/groups/organize` - Get grouped speakers

### Playback
- `POST /api/playback/speakers/{speakerMac}/play` - Play track on speaker
- `POST /api/playback/groups/{groupName}/play` - Play track on group
- `POST /api/playback/speakers/{speakerMac}/pause` - Pause speaker
- `POST /api/playback/groups/{groupName}/pause` - Pause group
- `POST /api/playback/speakers/{speakerMac}/resume` - Resume speaker
- `POST /api/playback/speakers/{speakerMac}/stop` - Stop speaker
- `PUT /api/playback/speakers/{speakerMac}/volume` - Set speaker volume
- `PUT /api/playback/groups/{groupName}/volume` - Set group volume
- `PUT /api/playback/speakers/{speakerMac}/seek` - Seek position on speaker
- `GET /api/playback/speakers/{speakerMac}/state` - Get playback state

### Library
- `GET /api/library/tracks` - Get all tracks
- `GET /api/library/tracks/{id}` - Get specific track
- `GET /api/library/search?query={query}` - Search tracks
- `POST /api/library/tracks` - Add track to library
- `PUT /api/library/tracks/{id}` - Update track
- `DELETE /api/library/tracks/{id}` - Delete track
- `GET /api/library/config/directory` - Get current music library directory
- `POST /api/library/config/directory` - Set music library directory
- `POST /api/library/scan` - Scan directory and add audio files to library
- `GET /api/library/files` - Get list of audio files in music library directory

## Project Structure

```
multiroom-app/
├── backend/
│   ├── src/main/java/com/samsung/multiroom/
│   │   ├── MultiroomApplication.java
│   │   ├── controller/
│   │   │   ├── SpeakersController.java
│   │   │   ├── GroupsController.java
│   │   │   ├── PlaybackController.java
│   │   │   └── LibraryController.java
│   │   ├── model/
│   │   │   ├── Speaker.java
│   │   │   ├── SpeakerGroup.java
│   │   │   └── Track.java
│   │   ├── repository/
│   │   │   ├── SpeakerRepository.java
│   │   │   ├── SpeakerGroupRepository.java
│   │   │   └── TrackRepository.java
│   │   └── service/
│   │       ├── DeviceDiscoveryService.java
│   │       ├── AudioPlaybackService.java
│   │       └── SpeakerGroupService.java
│   └── src/main/resources/
│       ├── application.properties
│       └── static/ (web UI files)
├── web-ui/
│   ├── index.html
│   ├── css/style.css
│   ├── js/
│   │   ├── app.js
│   │   ├── ui.js
│   │   └── api.js
│   └── images/
├── server.sh              # Server control script
├── test.html              # API test page
├── SUMMARY.md             # Project summary
└── README.md              # This file
```

## Development

### Backend Development

To run the application in development mode with automatic restart on code changes:

```bash
cd multiroom-app/backend
mvn spring-boot:run
```

### Frontend Development

The frontend files are located in the `multiroom-app/web-ui` directory and are served by the Spring Boot application from the `src/main/resources/static` directory.

## Configuration

The application uses the following default configuration:

- Server Port: 8081
- Database: H2 in-memory database (for demo)
- Network Discovery: Enabled for local network
- CORS: Enabled for all origins (for development)

To customize the configuration, modify the `application.properties` file in the `src/main/resources` directory.

## Limitations

This implementation has some limitations:

1. Direct speaker control is simulated in this demo version (actual communication with Samsung speakers would require implementing proprietary protocols)
2. Audio synchronization between speakers may not be perfectly aligned in all network conditions
3. Some advanced Samsung-specific features may not be available
4. The application requires speakers to be on the same network as the server

## Troubleshooting

- If the server fails to start, check that port 8081 is not in use by another application
- Ensure Java 11+ is installed and properly configured
- If speakers are not discovered, verify that the application server and speakers are on the same subnet
- Check the server logs for detailed error information

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Make your changes
4. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
5. Push to the branch (`git push origin feature/AmazingFeature`)
6. Open a Pull Request

## License

This project is for educational purposes only. Samsung and the Samsung multiroom audio application are trademarks of Samsung Electronics.