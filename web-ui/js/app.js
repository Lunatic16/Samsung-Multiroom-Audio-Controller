// Main application logic for Samsung Multiroom Audio Controller

document.addEventListener('DOMContentLoaded', function() {
    // Initialize the application
    initializeApp();
});

let currentView = 'dashboard'; // Default view
let currentSpeakers = [];
let currentGroups = [];
let currentTracks = [];
let currentPlaybackState = null;

async function initializeApp() {
    // Bind event listeners
    bindEventListeners();
    
    // Load initial data
    await loadSpeakers();
    await loadGroups();
    await loadTracks();
    
    // Start periodic updates
    startPeriodicUpdates();
}

function bindEventListeners() {
    // Navigation
    document.getElementById('dashboard-link').addEventListener('click', () => switchView('dashboard'));
    document.getElementById('speakers-link').addEventListener('click', () => switchView('speakers'));
    document.getElementById('groups-link').addEventListener('click', () => switchView('groups'));
    document.getElementById('library-link').addEventListener('click', () => switchView('library'));
    
    // Refresh devices
    document.getElementById('refresh-devices').addEventListener('click', async () => {
        await refreshDevices();
    });
    
    // Playback controls
    document.getElementById('play-btn').addEventListener('click', playTrack);
    document.getElementById('pause-btn').addEventListener('click', pauseTrack);
    document.getElementById('stop-btn').addEventListener('click', stopTrack);
    document.getElementById('prev-btn').addEventListener('click', prevTrack);
    document.getElementById('next-btn').addEventListener('click', nextTrack);
    
    // Volume control
    const volumeControl = document.getElementById('volume-control');
    volumeControl.addEventListener('input', (e) => {
        const volume = parseInt(e.target.value);
        updateVolumeDisplay(volume);
        // We'll implement volume change in the context of current playing speakers/groups
    });
    
    // Progress bar
    const progressBar = document.getElementById('progress-bar');
    progressBar.addEventListener('input', (e) => {
        const percent = parseInt(e.target.value);
        // Calculate position based on track duration
        if (currentPlaybackState && currentPlaybackState.duration) {
            const position = Math.floor((percent / 100) * currentPlaybackState.duration);
            // Seek to position in current playing context
        }
    });
    
    // Search functionality
    const searchBtn = document.getElementById('search-btn');
    const searchInput = document.getElementById('search-input');
    searchBtn.addEventListener('click', searchTracks);
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchTracks();
        }
    });
    
    // Add group button
    document.getElementById('add-group-btn').addEventListener('click', showAddGroupModal);
    
    // Save group button
    document.getElementById('save-group').addEventListener('click', createGroup);
}

async function switchView(viewName) {
    // Hide all views
    document.getElementById('dashboard-view').style.display = 'none';
    document.getElementById('speakers-view').style.display = 'none';
    document.getElementById('groups-view').style.display = 'none';
    document.getElementById('library-view').style.display = 'none';
    
    // Show selected view
    document.getElementById(`${viewName}-view`).style.display = 'block';
    
    // Update active nav link
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });
    document.getElementById(`${viewName}-link`).classList.add('active');
    
    currentView = viewName;
    
    // Load data specific to the view
    if (viewName === 'speakers') {
        await loadSpeakers();
    } else if (viewName === 'groups') {
        await loadGroups();
    } else if (viewName === 'library') {
        await loadTracks();
    }
}

async function loadSpeakers() {
    try {
        showLoading(document.getElementById('speakers-container'));
        const speakers = await api.getSpeakers();
        currentSpeakers = speakers;
        
        const container = document.getElementById('speakers-container');
        container.innerHTML = '';
        
        if (speakers.length === 0) {
            container.innerHTML = `
                <div class="col-12">
                    <div class="text-center py-5">
                        <i class="fas fa-soundcloud fa-3x text-muted mb-3"></i>
                        <h5>No speakers found</h5>
                        <p class="text-muted">Click refresh to search for speakers on your network</p>
                        <button class="btn btn-primary" id="refresh-devices-btn">Refresh Devices</button>
                    </div>
                </div>
            `;
            document.getElementById('refresh-devices-btn').addEventListener('click', refreshDevices);
        } else {
            speakers.forEach(speaker => {
                container.innerHTML += createSpeakerCard(speaker);
            });
            
            // Add event listeners to speaker cards
            document.querySelectorAll('.speaker-card').forEach(card => {
                card.addEventListener('click', () => {
                    const macAddress = card.getAttribute('data-mac');
                    // In a real implementation, this would show speaker details or play controls
                    console.log('Selected speaker:', macAddress);
                });
            });
        }
    } catch (error) {
        showError(document.getElementById('speakers-container'), 'Failed to load speakers');
    }
}

async function loadGroups() {
    try {
        showLoading(document.getElementById('groups-container'));
        const groupedData = await api.getGroupedSpeakers();
        currentGroups = groupedData.allGroups || [];
        
        const container = document.getElementById('groups-container');
        container.innerHTML = '';
        
        if (currentGroups.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5">
                    <i class="fas fa-users fa-3x text-muted mb-3"></i>
                    <h5>No speaker groups created</h5>
                    <p class="text-muted">Create a group to control multiple speakers together</p>
                    <button class="btn btn-primary" id="create-first-group">Create First Group</button>
                </div>
            `;
            document.getElementById('create-first-group').addEventListener('click', showAddGroupModal);
        } else {
            const groupsHtml = currentGroups.map(group => createGroupCard(group)).join('');
            container.innerHTML = `<div class="row">${groupsHtml}</div>`;
            
            // Add event listeners to group cards
            document.querySelectorAll('.play-group-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    const groupName = e.target.getAttribute('data-group');
                    // For demo, play the first track on the group
                    if (currentTracks.length > 0) {
                        await api.playOnGroup(groupName, currentTracks[0].id);
                        showToast(`Playing on group ${groupName}`, 'success');
                    }
                });
            });
            
            document.querySelectorAll('.delete-group-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    const groupName = e.target.getAttribute('data-group');
                    if (confirm(`Are you sure you want to delete group ${groupName}?`)) {
                        try {
                            await api.deleteGroup(groupName);
                            await loadGroups(); // Reload groups
                            showToast(`Group ${groupName} deleted`, 'success');
                        } catch (error) {
                            showToast('Failed to delete group', 'error');
                        }
                    }
                });
            });
        }
    } catch (error) {
        showError(document.getElementById('groups-container'), 'Failed to load groups');
    }
}

async function loadTracks() {
    try {
        showLoading(document.getElementById('tracks-container'));
        const tracks = await api.getTracks();
        currentTracks = tracks;
        
        const container = document.getElementById('tracks-container');
        container.innerHTML = '';
        
        if (tracks.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5">
                    <i class="fas fa-music fa-3x text-muted mb-3"></i>
                    <h5>No tracks in library</h5>
                    <p class="text-muted">Add music files to your library to get started</p>
                </div>
            `;
        } else {
            let tracksHtml = '<div class="list-group">';
            tracks.forEach((track, index) => {
                tracksHtml += createTrackItem(track, index);
            });
            tracksHtml += '</div>';
            container.innerHTML = tracksHtml;
            
            // Add event listeners to play track buttons
            document.querySelectorAll('.play-track-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    const trackId = parseInt(e.target.getAttribute('data-track-id'));
                    // For demo, play on the first available speaker
                    if (currentSpeakers.length > 0) {
                        await api.playOnSpeaker(currentSpeakers[0].macAddress, trackId);
                        showToast(`Playing track on ${currentSpeakers[0].name}`, 'success');
                    }
                });
            });
        }
    } catch (error) {
        showError(document.getElementById('tracks-container'), 'Failed to load tracks');
    }
}

async function refreshDevices() {
    try {
        showLoading(document.getElementById('speakers-container'));
        await api.refreshDevices();
        await loadSpeakers();
        showToast('Devices refreshed successfully', 'success');
    } catch (error) {
        showToast('Failed to refresh devices', 'error');
    }
}

async function searchTracks() {
    const query = document.getElementById('search-input').value.trim();
    if (!query) {
        await loadTracks();
        return;
    }
    
    try {
        showLoading(document.getElementById('tracks-container'));
        const tracks = await api.searchTracks(query);
        currentTracks = tracks;
        
        const container = document.getElementById('tracks-container');
        container.innerHTML = '';
        
        if (tracks.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5">
                    <i class="fas fa-search fa-3x text-muted mb-3"></i>
                    <h5>No tracks found</h5>
                    <p class="text-muted">No tracks match your search query "${query}"</p>
                </div>
            `;
        } else {
            let tracksHtml = '<div class="list-group">';
            tracks.forEach((track, index) => {
                tracksHtml += createTrackItem(track, index);
            });
            tracksHtml += '</div>';
            container.innerHTML = tracksHtml;
            
            // Add event listeners to play track buttons
            document.querySelectorAll('.play-track-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    const trackId = parseInt(e.target.getAttribute('data-track-id'));
                    // For demo, play on the first available speaker
                    if (currentSpeakers.length > 0) {
                        await api.playOnSpeaker(currentSpeakers[0].macAddress, trackId);
                        showToast(`Playing track on ${currentSpeakers[0].name}`, 'success');
                    }
                });
            });
        }
    } catch (error) {
        showError(document.getElementById('tracks-container'), 'Failed to search tracks');
    }
}

async function playTrack() {
    // For demo, play on the first available speaker
    if (currentSpeakers.length > 0 && currentTracks.length > 0) {
        try {
            await api.playOnSpeaker(currentSpeakers[0].macAddress, currentTracks[0].id);
            showToast(`Playing track on ${currentSpeakers[0].name}`, 'success');
        } catch (error) {
            showToast('Failed to play track', 'error');
        }
    }
}

async function pauseTrack() {
    // For demo, pause the first speaker
    if (currentSpeakers.length > 0) {
        try {
            await api.pauseSpeaker(currentSpeakers[0].macAddress);
            showToast(`Paused on ${currentSpeakers[0].name}`, 'success');
        } catch (error) {
            showToast('Failed to pause track', 'error');
        }
    }
}

async function stopTrack() {
    // For demo, stop on the first speaker
    if (currentSpeakers.length > 0) {
        try {
            await api.stopSpeaker(currentSpeakers[0].macAddress);
            showToast(`Stopped on ${currentSpeakers[0].name}`, 'success');
        } catch (error) {
            showToast('Failed to stop track', 'error');
        }
    }
}

function prevTrack() {
    showToast('Previous track functionality not implemented in demo', 'info');
}

function nextTrack() {
    showToast('Next track functionality not implemented in demo', 'info');
}

function showAddGroupModal() {
    // Populate speaker checkboxes
    const checkboxesContainer = document.getElementById('speaker-checkboxes');
    checkboxesContainer.innerHTML = '';
    
    if (currentSpeakers.length === 0) {
        checkboxesContainer.innerHTML = '<p class="text-muted">No speakers available to add to group</p>';
        return;
    }
    
    currentSpeakers.forEach(speaker => {
        checkboxesContainer.innerHTML += `
            <div class="form-check">
                <input class="form-check-input" type="checkbox" value="${speaker.macAddress}" id="speaker-${speaker.macAddress}">
                <label class="form-check-label" for="speaker-${speaker.macAddress}">
                    ${speaker.name} (${speaker.macAddress})
                </label>
            </div>
        `;
    });
    
    // Show the modal
    const modal = new bootstrap.Modal(document.getElementById('addGroupModal'));
    modal.show();
}

async function createGroup() {
    const groupName = document.getElementById('group-name').value.trim();
    if (!groupName) {
        showToast('Please enter a group name', 'error');
        return;
    }
    
    // Get selected speakers
    const checkboxes = document.querySelectorAll('#speaker-checkboxes input[type="checkbox"]:checked');
    const selectedSpeakers = Array.from(checkboxes).map(cb => cb.value);
    
    if (selectedSpeakers.length === 0) {
        showToast('Please select at least one speaker for the group', 'error');
        return;
    }
    
    try {
        await api.createGroup(groupName, selectedSpeakers);
        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('addGroupModal'));
        modal.hide();
        
        // Clear form
        document.getElementById('group-name').value = '';
        
        // Reload groups
        await loadGroups();
        showToast(`Group "${groupName}" created successfully`, 'success');
    } catch (error) {
        showToast('Failed to create group', 'error');
    }
}

function startPeriodicUpdates() {
    // Update the UI periodically
    setInterval(async () => {
        if (currentView === 'speakers') {
            await loadSpeakers(); // Refresh speakers list
        }
    }, 30000); // Refresh every 30 seconds
}

// Show toast notification
function showToast(message, type = 'info') {
    // Remove existing toasts
    const existingToasts = document.querySelectorAll('.toast-container');
    existingToasts.forEach(toast => toast.remove());
    
    // Create toast container if it doesn't exist
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = 1000;
        document.body.appendChild(toastContainer);
    }
    
    // Create toast element
    const toastEl = document.createElement('div');
    toastEl.className = 'toast show';
    toastEl.setAttribute('role', 'alert');
    toastEl.innerHTML = `
        <div class="toast-header">
            <strong class="me-auto">
                ${type === 'success' ? '<i class="fas fa-check-circle text-success me-2"></i> Success' : 
                  type === 'error' ? '<i class="fas fa-exclamation-circle text-danger me-2"></i> Error' : 
                  '<i class="fas fa-info-circle text-info me-2"></i> Info'}
            </strong>
            <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
        </div>
        <div class="toast-body">
            ${message}
        </div>
    `;
    
    toastContainer.appendChild(toastEl);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (toastEl.parentNode) {
            toastEl.remove();
        }
    }, 5000);
}

// Initialize with dashboard view
switchView('dashboard');