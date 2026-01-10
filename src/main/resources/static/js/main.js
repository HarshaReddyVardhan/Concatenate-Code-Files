/**
 * Main application logic
 */

document.addEventListener('DOMContentLoaded', () => {
    // Session-only persistence
    const savedPath = sessionStorage.getItem('projectPath');
    if (savedPath) {
        const el = document.getElementById('projectPath');
        if (el) el.value = savedPath;
    }

    // Check environment (Docker/Headless)
    checkEnvironment();
});

async function checkEnvironment() {
    try {
        const response = await fetch('/api/utils/env-info');
        if (response.ok) {
            const env = await response.json();
            // If Docker or Headless, hide browse buttons and show hint
            if (env.isDocker || env.isHeadless) {
                const browseBtns = document.querySelectorAll('.browse-btn');
                browseBtns.forEach(btn => btn.style.display = 'none');

                const dockerHint = document.getElementById('dockerHint');
                if (dockerHint) dockerHint.style.display = 'block';

                // Hide the taskbar hint message as well since it's irrelevant
                const browseHint = document.getElementById('browseHint');
                if (browseHint) browseHint.style.display = 'none'; // Ensure it stays hidden
            }
        }
    } catch (e) {
        console.warn("Failed to check environment:", e);
    }
}

// Save path on input (Session Storage)
document.getElementById('projectPath')?.addEventListener('input', (e) => {
    sessionStorage.setItem('projectPath', e.target.value);
});

// Handle Folder Selection with taskbar hint parameter
async function browseFolder(targetInputId) {
    // If no ID provided, default to projectPath for backward compatibility (though we updated calls)
    if (!targetInputId) targetInputId = 'projectPath';

    const hintElement = document.getElementById('browseHint');
    // Find the specific button that triggered this to show spinner
    // We assume the button is the one with the onclick that matches or we can find it relative to input
    // Simpler: just find the button that calls this function with this arg
    const browseBtn = document.querySelector(`button[onclick*="'${targetInputId}'"]`);

    let hintTimeout = null;

    try {
        // Disable button while waiting
        if (browseBtn) {
            browseBtn.disabled = true;
            browseBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        }

        // Show hint after 2 seconds if dialog hasn't returned
        hintTimeout = setTimeout(() => {
            if (hintElement) {
                hintElement.style.display = 'block';
                hintElement.classList.add('pulse-animation');
            }
            // Also show a non-blocking toast notification
            showNotification('Please check your taskbar for the folder selection window.', 'warning');
        }, 2000);

        const response = await fetch('/api/utils/browse-folder');

        // Clear the timeout since we got a response
        clearTimeout(hintTimeout);
        if (hintElement) {
            hintElement.style.display = 'none';
            hintElement.classList.remove('pulse-animation');
        }

        // Restore button
        if (browseBtn) {
            browseBtn.disabled = false;
            browseBtn.innerHTML = '<i class="fas fa-folder-open"></i>';
        }

        if (response.ok) {
            const data = await response.json();
            if (data.path) {
                const pathInput = document.getElementById(targetInputId);
                if (pathInput) {
                    pathInput.value = data.path;
                    // Trigger input event to save to session storage (only relevant for projectPath)
                    pathInput.dispatchEvent(new Event('input'));
                }
                showNotification('Folder selected successfully!', 'success');
            } else if (data.error) {
                console.error("Browse error:", data.error);
                showNotification("Error: " + data.error, 'error');
            }
            // else: user cancelled, do nothing
        } else {
            const errorData = await response.json();
            console.error("Browse endpoint returned error:", errorData);
            showNotification("Error: " + (errorData.error || "Unknown error occurred"), 'error');
        }
    } catch (err) {
        clearTimeout(hintTimeout);
        if (hintElement) {
            hintElement.style.display = 'none';
            hintElement.classList.remove('pulse-animation');
        }
        // Restore button
        if (browseBtn) {
            browseBtn.disabled = false;
            browseBtn.innerHTML = '<i class="fas fa-folder-open"></i>';
        }
        console.error("Browse Error:", err);
        showNotification("Could not open folder picker. Please enter the path manually.", 'error');
    }
}

// Toast notification helper
function showNotification(message, type = 'info') {
    // Remove existing notifications
    const existing = document.querySelector('.toast-notification');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `toast-notification toast-${type}`;

    const icons = {
        success: 'fa-check-circle',
        error: 'fa-exclamation-circle',
        warning: 'fa-exclamation-triangle',
        info: 'fa-info-circle'
    };

    toast.innerHTML = `
        <i class="fas ${icons[type] || icons.info}"></i>
        <span>${message}</span>
        <button onclick="this.parentElement.remove()" style="background:none;border:none;color:inherit;cursor:pointer;margin-left:10px;">
            <i class="fas fa-times"></i>
        </button>
    `;

    document.body.appendChild(toast);

    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (toast.parentElement) {
            toast.style.animation = 'slideOut 0.3s ease forwards';
            setTimeout(() => toast.remove(), 300);
        }
    }, 5000);
}


// Form Submission
async function generateConcatenation(incremental = false) {
    const projectPath = document.getElementById('projectPath').value;
    const outputFolder = document.getElementById('outputFolder').value;
    const maxFileSizeMb = document.getElementById('maxFileSizeMb').value;

    // Feature flags
    const useXmlTags = document.getElementById('useXmlTags').checked;
    const includeFileTree = document.getElementById('includeFileTree').checked;
    const estimateTokens = document.getElementById('estimateTokens').checked;

    if (!projectPath) {
        alert('Please enter a project path');
        return;
    }

    setLoading(true);

    const requestData = {
        projectPath: projectPath.replace(/"/g, ''), // Remove quotes if user copied as path
        outputFolder: outputFolder || null,
        maxFileSizeMb: maxFileSizeMb ? parseInt(maxFileSizeMb) : null,
        incrementalUpdate: incremental,
        useXmlTags,
        includeFileTree,
        estimateTokens
    };

    try {
        const endpoint = incremental ? '/api/update' : '/api/generate';
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        });

        const data = await response.json();
        setLoading(false);
        displayResult(data);

    } catch (error) {
        setLoading(false);
        displayError(error.message);
    }
}

function setLoading(isLoading) {
    const loader = document.getElementById('loading');
    const result = document.getElementById('result');
    const placeholder = document.getElementById('placeholder');

    if (isLoading) {
        loader.style.display = 'block';
        if (result) result.style.display = 'none';
        if (placeholder) placeholder.style.display = 'none';
    } else {
        loader.style.display = 'none';
    }
}

function displayResult(data) {
    const container = document.getElementById('result');
    container.style.display = 'block';

    if (data.success) {
        const processingTimeSec = (data.processingTimeMs / 1000).toFixed(2);
        const totalSizeMb = (data.totalSizeBytes / (1024 * 1024)).toFixed(2);

        container.className = 'card result-card success';
        container.innerHTML = `
            <div class="result-header">
                <div style="background:#def7ec; color:#03543f; width:40px; height:40px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:20px;">✓</div>
                <div>
                    <h3 style="color:#03543f; margin-bottom:2px;">Processing Complete</h3>
                    <p style="color:#046c4e; font-size:14px;">Your files are ready</p>
                </div>
            </div>

            <div class="stat-grid">
                <div class="stat-card">
                    <span class="stat-value">${data.totalFilesProcessed}</span>
                    <span class="stat-label">Files Processed</span>
                </div>
                <div class="stat-card">
                    <span class="stat-value">${totalSizeMb} MB</span>
                    <span class="stat-label">Total Size</span>
                </div>
                <div class="stat-card">
                    <span class="stat-value">${data.estimatedTokenCount ? parseInt(data.estimatedTokenCount).toLocaleString() : 'N/A'}</span>
                    <span class="stat-label">Est. Tokens</span>
                </div>
                <div class="stat-card">
                    <span class="stat-value">${processingTimeSec}s</span>
                    <span class="stat-label">Time</span>
                </div>
            </div>

            ${data.outputFiles && data.outputFiles.length > 0 ? `
                <h4 style="margin-bottom:10px;">Output Files</h4>
                <ul class="file-list">
                    ${data.outputFiles.map(file => `<li>${file}</li>`).join('')}
                </ul>
            ` : ''}
            
            ${data.previewFileTree ? `
                <h4 style="margin-top:20px; margin-bottom:10px;">Preview Structure</h4>
                <div class="file-tree-preview">${escapeHtml(data.previewFileTree)}</div>
            ` : ''}
        `;
    } else {
        container.className = 'card result-card error';
        container.innerHTML = `
            <div class="result-header">
                <div style="background:#fde8e8; color:#9b1c1c; width:40px; height:40px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:20px;">✕</div>
                <div>
                    <h3 style="color:#9b1c1c; margin-bottom:2px;">Operation Failed</h3>
                    <p style="color:#c81e1e; font-size:14px;">${data.message}</p>
                </div>
            </div>
            ${data.errors ? `<ul style="color:#c81e1e; margin-left:20px; font-size:14px;">${data.errors.map(e => `<li>${e}</li>`).join('')}</ul>` : ''}
        `;
    }
}

function displayError(msg) {
    const container = document.getElementById('result');
    container.style.display = 'block';
    container.innerHTML = `<div style="padding:20px; background:#fde8e8; color:#c81e1e; border-radius:8px;">${msg}</div>`;
}

function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
