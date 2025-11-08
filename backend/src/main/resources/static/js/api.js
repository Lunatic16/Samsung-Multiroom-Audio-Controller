// API client for Samsung Multiroom Audio Controller
// Handles all communication with the backend REST API

class MultiroomAPI {
    constructor() {
        this.baseUrl = 'http://localhost:8080/api'; // Update this with your backend URL
    }

    // Fetch all speakers
    async getSpeakers() {
        try {
            const response = await fetch(`${this.baseUrl}/speakers`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching speakers:', error);
            throw error;
        }
    }

    // Refresh device discovery
    async refreshDevices() {
        try {
            const response = await fetch(`${this.baseUrl}/speakers/refresh`, {
                method: 'POST'
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error refreshing devices:', error);
            throw error;
        }
    }

    // Get speaker by MAC address
    async getSpeaker(macAddress) {
        try {
            const response = await fetch(`${this.baseUrl}/speakers/${macAddress}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching speaker:', error);
            throw error;
        }
    }

    // Get all speaker groups
    async getGroups() {
        try {
            const response = await fetch(`${this.baseUrl}/groups`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching groups:', error);
            throw error;
        }
    }

    // Get grouped speakers
    async getGroupedSpeakers() {
        try {
            const response = await fetch(`${this.baseUrl}/groups/organize`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching grouped speakers:', error);
            throw error;
        }
    }

    // Create a new speaker group
    async createGroup(name, speakers) {
        try {
            const response = await fetch(`${this.baseUrl}/groups`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    name: name,
                    speakers: speakers
                })
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating group:', error);
            throw error;
        }
    }

    // Delete a speaker group
    async deleteGroup(name) {
        try {
            const response = await fetch(`${this.baseUrl}/groups/${name}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error deleting group:', error);
            throw error;
        }
    }

    // Play track on speaker
    async playOnSpeaker(speakerMac, trackId) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/speakers/${speakerMac}/play`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    trackId: trackId
                })
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error playing on speaker:', error);
            throw error;
        }
    }

    // Play track on group
    async playOnGroup(groupName, trackId) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/groups/${groupName}/play`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    trackId: trackId
                })
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error playing on group:', error);
            throw error;
        }
    }

    // Pause speaker
    async pauseSpeaker(speakerMac) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/speakers/${speakerMac}/pause`, {
                method: 'POST'
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error pausing speaker:', error);
            throw error;
        }
    }

    // Pause group
    async pauseGroup(groupName) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/groups/${groupName}/pause`, {
                method: 'POST'
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error pausing group:', error);
            throw error;
        }
    }

    // Resume speaker
    async resumeSpeaker(speakerMac) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/speakers/${speakerMac}/resume`, {
                method: 'POST'
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error resuming speaker:', error);
            throw error;
        }
    }

    // Stop speaker
    async stopSpeaker(speakerMac) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/speakers/${speakerMac}/stop`, {
                method: 'POST'
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error stopping speaker:', error);
            throw error;
        }
    }

    // Set speaker volume
    async setSpeakerVolume(speakerMac, volume) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/speakers/${speakerMac}/volume`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    volume: volume
                })
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error setting speaker volume:', error);
            throw error;
        }
    }

    // Set group volume
    async setGroupVolume(groupName, volume) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/groups/${groupName}/volume`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    volume: volume
                })
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error setting group volume:', error);
            throw error;
        }
    }

    // Seek position on speaker
    async seekPosition(speakerMac, seconds) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/speakers/${speakerMac}/seek`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    seconds: seconds
                })
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.text();
        } catch (error) {
            console.error('Error seeking position:', error);
            throw error;
        }
    }

    // Get playback state for speaker
    async getPlaybackState(speakerMac) {
        try {
            const response = await fetch(`${this.baseUrl}/playback/speakers/${speakerMac}/state`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error getting playback state:', error);
            throw error;
        }
    }

    // Get all tracks in library
    async getTracks() {
        try {
            const response = await fetch(`${this.baseUrl}/library/tracks`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching tracks:', error);
            throw error;
        }
    }

    // Search tracks
    async searchTracks(query) {
        try {
            const response = await fetch(`${this.baseUrl}/library/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error searching tracks:', error);
            throw error;
        }
    }
}

// Create global API instance
const api = new MultiroomAPI();