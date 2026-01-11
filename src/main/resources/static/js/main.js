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

    // Modal Event Listeners
    document.querySelector('.close-modal')?.addEventListener('click', closeFolderModal);
    document.getElementById('modalCancelBtn')?.addEventListener('click', closeFolderModal);
    document.getElementById('modalSelectBtn')?.addEventListener('click', confirmFolderSelection);
});

let envState = { isDocker: false, isHeadless: false };
let currentTargetInputId = null;
let currentBrowsePath = "/"; // Default start path for Docker

async function checkEnvironment() {
    try {
        const response = await fetch('/api/utils/env-info');
        if (response.ok) {
            const env = await response.json();
            envState = env;
            
            console.log("Environment state:", envState);

            if (env.isDocker || env.isHeadless) {
                 // Update placeholder to indicate Docker path format
                 const pathInput = document.getElementById('projectPath');
                 if (pathInput) pathInput.placeholder = "e.g. /workspace or /host/c/Projects...";
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

// Handle Folder Selection
async function browseFolder(targetInputId) {
    if (!targetInputId) targetInputId = 'projectPath';
    currentTargetInputId = targetInputId;

    if (envState.isDocker || envState.isHeadless) {
        // Use Web-Based Picker for Docker/Headless
        openFolderModal();
    } else {
        // Use Native Swing Picker for Local Desktop
        await browseNativeFolder(targetInputId);
    }
}

async function browseNativeFolder(targetInputId) {
    const hintElement = document.getElementById('browseHint');
    const browseBtn = document.querySelector(`button[onclick*="'${targetInputId}'"]`);
    let hintTimeout = null;

    try {
        if (browseBtn) {
            browseBtn.disabled = true;
            browseBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        }

        hintTimeout = setTimeout(() => {
            if (hintElement) {
                hintElement.style.display = 'block';
                hintElement.classList.add('pulse-animation');
            }
            showNotification('Please check your taskbar for the folder selection window.', 'warning');
        }, 2000);

        const response = await fetch('/api/utils/browse-folder');

        clearTimeout(hintTimeout);
        if (hintElement) {
            hintElement.style.display = 'none';
            hintElement.classList.remove('pulse-animation');
        }

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
                    pathInput.dispatchEvent(new Event('input'));
                }
                showNotification('Folder selected successfully!', 'success');
            } else if (data.error) {
                showNotification("Error: " + data.error, 'error');
            }
        } else {
            const errorData = await response.json();
            showNotification("Error: " + (errorData.error || "Unknown error"), 'error');
        }
    } catch (err) {
        clearTimeout(hintTimeout);
        if (hintElement) hintElement.style.display = 'none';
        if (browseBtn) {
            browseBtn.disabled = false;
            browseBtn.innerHTML = '<i class="fas fa-folder-open"></i>';
        }
        console.error("Browse Error:", err);
        showNotification("Could not open folder picker.", 'error');
    }
}

/* --- Web Folder Modal Logic --- */

function openFolderModal() {
    const modal = document.getElementById('folderModal');
    if(modal) {
        modal.classList.add('active');
        // Start at root or current value if valid
        const currentVal = document.getElementById(currentTargetInputId)?.value;
        currentBrowsePath = (currentVal && currentVal.startsWith('/')) ? currentVal : '/';
        loadModalPath(currentBrowsePath);
    }
}

function closeFolderModal() {
    const modal = document.getElementById('folderModal');
    if(modal) modal.classList.remove('active');
}

async function loadModalPath(path) {
    const listContainer = document.getElementById('modalFolderList');
    const breadcrumb = document.getElementById('modalBreadcrumb');
    const selectBtn = document.getElementById('modalSelectBtn');
    
    if(!listContainer) return;
    
    listContainer.innerHTML = '<div style="text-align:center; padding:20px;"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
    selectBtn.textContent = `Select: ${path}`;
    selectBtn.disabled = false;
    currentBrowsePath = path;

    // Update Breadcrumb
    const parts = path.split('/').filter(p => p);
    let breadHtml = `<span class="breadcrumb-item" onclick="loadModalPath('/')"><i class="fas fa-hdd"></i> root</span>`;
    
    let currentBuild = '';
    parts.forEach((part, index) => {
        currentBuild += '/' + part;
        // Capture value for closure
        const clickPath = currentBuild; 
        breadHtml += ` <span class="breadcrumb-separator">/</span> <span class="breadcrumb-item" onclick="loadModalPath('${clickPath}')">${part}</span>`;
    });
    breadcrumb.innerHTML = breadHtml;

    try {
        const res = await fetch(`/api/utils/list-dirs?path=${encodeURIComponent(path)}`);
        if(res.ok) {
            const files = await res.json();
            renderFolderList(files);
        } else {
            listContainer.innerHTML = '<div style="color:red; padding:10px;">Failed to load directory.</div>';
        }
    } catch(e) {
        console.error(e);
        listContainer.innerHTML = '<div style="color:red; padding:10px;">Error loading directory.</div>';
    }
}

function renderFolderList(files) {
    const container = document.getElementById('modalFolderList');
    container.innerHTML = '';
    
    // Parent link if not root
    if(currentBrowsePath !== '/' && currentBrowsePath !== '') {
         const parentPath = currentBrowsePath.substring(0, currentBrowsePath.lastIndexOf('/')) || '/';
         const div = document.createElement('div');
         div.className = 'folder-item';
         div.innerHTML = `<i class="fas fa-level-up-alt folder-icon"></i> <span>.. (Up one level)</span>`;
         div.onclick = () => loadModalPath(parentPath);
         container.appendChild(div);
    }

    if(files.length === 0) {
        const div = document.createElement('div');
        div.style.padding = "10px";
        div.style.color = "#718096";
        div.innerHTML = "No subdirectories found.";
        container.appendChild(div);
        return;
    }

    files.forEach(file => {
        const div = document.createElement('div');
        div.className = 'folder-item';
        div.innerHTML = `<i class="fas fa-folder folder-icon"></i> <span>${file.name}</span>`;
        div.onclick = () => loadModalPath(file.path);
        container.appendChild(div);
    });
}

function confirmFolderSelection() {
    if(currentTargetInputId && currentBrowsePath) {
        const input = document.getElementById(currentTargetInputId);
        input.value = currentBrowsePath;
        input.dispatchEvent(new Event('input'));
        closeFolderModal();
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
