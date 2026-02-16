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

    // Initial check for select button
    const path = document.getElementById('projectPath');
    const btn = document.getElementById('selectFilesBtn');
    if (path && path.value.trim() && btn) btn.disabled = false;
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
    const btn = document.getElementById('selectFilesBtn');
    if (btn) btn.disabled = !e.target.value.trim();
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
    if (modal) {
        modal.classList.add('active');
        // Start at root or current value if valid
        const currentVal = document.getElementById(currentTargetInputId)?.value;
        currentBrowsePath = (currentVal && currentVal.startsWith('/')) ? currentVal : '/';
        loadModalPath(currentBrowsePath);
    }
}

function closeFolderModal() {
    const modal = document.getElementById('folderModal');
    if (modal) modal.classList.remove('active');
}

async function loadModalPath(path) {
    const listContainer = document.getElementById('modalFolderList');
    const breadcrumb = document.getElementById('modalBreadcrumb');
    const selectBtn = document.getElementById('modalSelectBtn');

    if (!listContainer) return;

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
        if (res.ok) {
            const files = await res.json();
            renderFolderList(files);
        } else {
            listContainer.innerHTML = '<div style="color:red; padding:10px;">Failed to load directory.</div>';
        }
    } catch (e) {
        console.error(e);
        listContainer.innerHTML = '<div style="color:red; padding:10px;">Error loading directory.</div>';
    }
}

function renderFolderList(files) {
    const container = document.getElementById('modalFolderList');
    container.innerHTML = '';

    // Parent link if not root
    if (currentBrowsePath !== '/' && currentBrowsePath !== '') {
        const parentPath = currentBrowsePath.substring(0, currentBrowsePath.lastIndexOf('/')) || '/';
        const div = document.createElement('div');
        div.className = 'folder-item';
        div.innerHTML = `<i class="fas fa-level-up-alt folder-icon"></i> <span>.. (Up one level)</span>`;
        div.onclick = () => loadModalPath(parentPath);
        container.appendChild(div);
    }

    if (files.length === 0) {
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
    if (currentTargetInputId && currentBrowsePath) {
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
    const includeFileHeader = document.getElementById('includeFileHeader').checked;
    const removeComments = document.getElementById('removeComments').checked;
    const removeRedundantWhitespace = document.getElementById('removeRedundantWhitespace').checked;
    const minify = document.getElementById('minify').checked;

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
        estimateTokens,
        includeFileHeader,
        removeComments,
        removeRedundantWhitespace,
        minify,
        selectedFilePaths: (selectedFilePaths && selectedFilePaths.length > 0) ? selectedFilePaths : null
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

            ${data.processedFilePaths && data.processedFilePaths.length > 0 ? `
                 <details style="margin-top:15px; border:1px solid #e2e8f0; border-radius:6px; padding:10px;">
                    <summary style="cursor:pointer; font-weight:600; color:#2c5282;">
                        Files Included (${data.processedFilePaths.length})
                    </summary>
                    <div style="margin-top:10px; max-height:200px; overflow-y:auto; font-size:12px; color:#4a5568;">
                        <ul style="list-style:none; padding:0; margin:0;">
                            ${data.processedFilePaths.map(f => `<li style="padding:2px 0; border-bottom:1px solid #eee;">${f}</li>`).join('')}
                        </ul>
                    </div>
                </details>
            ` : (data.totalFilesProcessed > 0 ? `
                 <div style="margin-top:15px; font-size:13px; color:#718096;">
                    ${data.totalFilesProcessed} files processed.
                 </div>
            ` : '')}
            
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

/* --- File Selection Tree Logic --- */

let selectedFilePaths = []; // Global store for selected files relative paths
let loadedFileTree = null; // Cache tree data

async function openFileSelectionModal() {
    const projectPath = document.getElementById('projectPath').value;
    if (!projectPath) {
        showNotification("Please enter a project path first.", "warning");
        return;
    }

    const modal = document.getElementById('fileSelectionModal');
    if (modal) modal.classList.add('active');

    // Load tree if not loaded or path changed (simple check: always reload for now to catch updates)
    await loadFileTree(projectPath);
}

function closeFileSelectionModal() {
    const modal = document.getElementById('fileSelectionModal');
    if (modal) modal.classList.remove('active');
}

async function loadFileTree(path) {
    const container = document.getElementById('fileTreeContainer');
    container.innerHTML = '<div style="text-align:center; padding:20px; color:#a0aec0;"><i class="fas fa-spinner fa-spin"></i> Loading file structure...</div>';

    try {
        const res = await fetch(`/api/utils/file-tree?path=${encodeURIComponent(path)}`);
        if (res.ok) {
            loadedFileTree = await res.json();
            renderFileTree(loadedFileTree, container);
            // Restore selections
            restoreSelections();
            updateSelectionSummary();
        } else {
            container.innerHTML = '<div style="color:red; padding:20px;">Failed to load file tree. Check path and permissions.</div>';
        }
    } catch (e) {
        console.error(e);
        container.innerHTML = '<div style="color:red; padding:20px;">Error loading file tree.</div>';
    }
}

function renderFileTree(nodes, container) {
    container.innerHTML = '';
    if (!nodes || nodes.length === 0) {
        container.innerHTML = '<div style="padding:10px;">No files found.</div>';
        return;
    }
    const ul = document.createElement('ul');
    ul.className = 'tree-ul';
    nodes.forEach(node => {
        ul.appendChild(createTreeNode(node));
    });
    container.appendChild(ul);
}

function createTreeNode(node) {
    const li = document.createElement('li');
    li.className = 'tree-li';

    const contentDiv = document.createElement('div');
    contentDiv.className = 'tree-item-content';

    const isDir = node.type === 'directory';

    // Caret
    const caret = document.createElement('span');
    caret.className = 'tree-caret';
    if (isDir) {
        caret.innerHTML = '<i class="fas fa-chevron-right"></i>';
        caret.onclick = (e) => {
            e.stopPropagation();
            toggleNode(li, caret);
        };
    } else {
        caret.innerHTML = '';
        caret.style.cursor = 'default';
    }
    contentDiv.appendChild(caret);

    // Checkbox
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'tree-checkbox';
    // Use path as value
    checkbox.value = node.path;
    checkbox.dataset.type = node.type;
    checkbox.onclick = (e) => handleCheckboxClick(e, li);
    contentDiv.appendChild(checkbox);

    // Icon
    const icon = document.createElement('span');
    icon.className = `tree-icon ${isDir ? 'folder' : 'file'}`;
    icon.innerHTML = isDir ? '<i class="fas fa-folder"></i>' : '<i class="fas fa-file-code"></i>';
    contentDiv.appendChild(icon);

    // Label
    const label = document.createElement('span');
    label.className = 'tree-label';
    label.textContent = node.name;
    label.title = node.path;
    label.onclick = () => {
        if (isDir) toggleNode(li, caret);
        else checkbox.click();
    };
    contentDiv.appendChild(label);

    li.appendChild(contentDiv);

    // Children
    if (isDir && node.children) {
        const childrenContainer = document.createElement('div');
        childrenContainer.className = 'tree-children';
        const childUl = document.createElement('ul');
        childUl.className = 'tree-ul';
        node.children.forEach(child => {
            childUl.appendChild(createTreeNode(child));
        });
        childrenContainer.appendChild(childUl);
        li.appendChild(childrenContainer);
    }

    return li;
}

function toggleNode(li, caret) {
    const children = li.querySelector('.tree-children');
    if (children) {
        children.classList.toggle('active');
        caret.classList.toggle('tree-caret-down');
    }
}

function handleCheckboxClick(e, li) {
    const checkbox = e.target;
    const isChecked = checkbox.checked;
    const isDir = checkbox.dataset.type === 'directory';

    if (isDir) {
        const children = li.querySelectorAll('input[type="checkbox"]');
        children.forEach(child => child.checked = isChecked);
    }

    updateSelectionSummary();
}

function toggleAllFiles(select) {
    const checkboxes = document.querySelectorAll('#fileTreeContainer input[type="checkbox"]');
    checkboxes.forEach(cb => cb.checked = select);
    updateSelectionSummary();
}

function updateSelectionSummary() {
    const checkboxes = document.querySelectorAll('#fileTreeContainer input[type="checkbox"][data-type="file"]');
    const checked = Array.from(checkboxes).filter(cb => cb.checked);
    document.getElementById('selectionSummary').textContent = `${checked.length} files selected`;
}

function confirmFileSelection() {
    const checkboxes = document.querySelectorAll('#fileTreeContainer input[type="checkbox"][data-type="file"]');
    selectedFilePaths = Array.from(checkboxes)
        .filter(cb => cb.checked)
        .map(cb => cb.value);

    const hint = document.getElementById('fileSelectionHint');
    const countSpan = document.getElementById('fileSelectionCount');

    const totalFiles = checkboxes.length;
    if (selectedFilePaths.length === totalFiles) {
        hint.style.display = 'block';
        countSpan.textContent = "All files";
        selectedFilePaths = [];
    } else if (selectedFilePaths.length > 0) {
        hint.style.display = 'block';
        countSpan.textContent = `${selectedFilePaths.length} files`;
    } else {
        hint.style.display = 'block';
        countSpan.textContent = "0 files (None)";
    }

    closeFileSelectionModal();
    showNotification("File selection updated", "success");
}

function restoreSelections() {
    if (selectedFilePaths.length === 0) {
        toggleAllFiles(true);
        return;
    }

    const checkboxes = document.querySelectorAll('#fileTreeContainer input[type="checkbox"]');
    const selectedSet = new Set(selectedFilePaths);

    checkboxes.forEach(cb => {
        if (cb.dataset.type === 'file') {
            cb.checked = selectedSet.has(cb.value);
        }
    });
}
