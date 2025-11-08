// UI utility functions for Samsung Multiroom Audio Controller

// Format seconds to MM:SS
function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
}

// Create speaker card element
function createSpeakerCard(speaker) {
    const statusClass = speaker.status === 'PLAYING' ? 'playing' : '';
    const statusIcon = speaker.status === 'PLAYING' ? 'status-playing' : 
                       speaker.status === 'PAUSED' ? 'status-paused' : 
                       speaker.status === 'STOPPED' ? 'status-stopped' : 'status-disconnected';
    
    return `
        <div class="col-md-4 mb-3">
            <div class="card speaker-card ${statusClass}" data-mac="${speaker.macAddress}">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6 class="card-title mb-0">${speaker.name || 'Unknown Speaker'}</h6>
                        <span class="status-indicator ${statusIcon}"></span>
                    </div>
                    <p class="card-text text-muted small mb-1">
                        <i class="fas fa-microchip me-1"></i> ${speaker.model || 'Model Unknown'}
                    </p>
                    <p class="card-text text-muted small mb-2">
                        <i class="fas fa-network-wired me-1"></i> ${speaker.ipAddress || 'IP Unknown'}
                    </p>
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <i class="fas fa-volume-up me-1"></i>
                            <small>${speaker.volume}%</small>
                        </div>
                        <div>
                            <small class="text-muted">${speaker.currentTrack ? 'Playing' : 'Idle'}</small>
                        </div>
                    </div>
                    ${speaker.currentTrack ? `
                    <div class="mt-2">
                        <div class="progress" style="height: 5px;">
                            <div class="progress-bar" role="progressbar" style="width: ${Math.random() * 100}%"></div>
                        </div>
                        <small class="text-truncate d-block">${speaker.currentTrack}</small>
                    </div>
                    ` : ''}
                </div>
            </div>
        </div>
    `;
}

// Create group card element
function createGroupCard(group) {
    return `
        <div class="col-md-6 mb-3">
            <div class="card group-card" data-group="${group.name}">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6 class="card-title mb-0">${group.name}</h6>
                        <span class="badge bg-primary">${group.speakerMacAddresses ? group.speakerMacAddresses.length : 0} speakers</span>
                    </div>
                    <div class="d-flex justify-content-between">
                        <button class="btn btn-sm btn-primary play-group-btn" data-group="${group.name}">
                            <i class="fas fa-play"></i> Play
                        </button>
                        <button class="btn btn-sm btn-outline-danger delete-group-btn" data-group="${group.name}">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// Create track item element
function createTrackItem(track, index) {
    return `
        <div class="row track-item align-items-center" data-track-id="${track.id}">
            <div class="col-1">
                <span class="text-muted">${index + 1}</span>
            </div>
            <div class="col-4">
                <div class="d-flex align-items-center">
                    <img src="images/album-placeholder.jpg" alt="Album Art" width="40" height="40" class="me-2">
                    <div>
                        <div class="fw-bold">${track.title}</div>
                        <div class="text-muted small">${track.artist}</div>
                    </div>
                </div>
            </div>
            <div class="col-3">
                ${track.album}
            </div>
            <div class="col-2">
                ${formatTime(track.duration || 0)}
            </div>
            <div class="col-2">
                <button class="btn btn-sm btn-primary play-track-btn" data-track-id="${track.id}">
                    <i class="fas fa-play"></i> Play
                </button>
            </div>
        </div>
    `;
}

// Show loading state
function showLoading(element) {
    element.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2">Loading...</p>
        </div>
    `;
}

// Show error message
function showError(element, message) {
    element.innerHTML = `
        <div class="alert alert-danger" role="alert">
            <i class="fas fa-exclamation-triangle me-2"></i>
            ${message}
        </div>
    `;
}

// Show success message
function showSuccess(element, message) {
    element.innerHTML = `
        <div class="alert alert-success" role="alert">
            <i class="fas fa-check-circle me-2"></i>
            ${message}
        </div>
    `;
}

// Format bytes to human readable format
function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

// Debounce function to limit API calls
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Update volume display
function updateVolumeDisplay(volume) {
    document.getElementById('volume-display').textContent = `${volume}%`;
    document.getElementById('volume-control').value = volume;
}