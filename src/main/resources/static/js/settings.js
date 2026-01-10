// Load settings on page load
window.addEventListener('DOMContentLoaded', loadSettings);

async function loadSettings() {
    try {
        const response = await fetch('/api/settings');
        const settings = await response.json();

        // Populate exclude patterns
        const excludeList = document.getElementById('excludePatternsList');
        excludeList.innerHTML = '';
        settings.excludePatterns.forEach(pattern => {
            excludeList.innerHTML += `
                <div class="tag">
                    ${pattern}
                    <button onclick="removeExcludePattern('${pattern}')">×</button>
                </div>
            `;
        });

        // Populate include extensions
        const includeList = document.getElementById('includeExtensionsList');
        includeList.innerHTML = '';
        settings.includeExtensions.forEach(ext => {
            includeList.innerHTML += `
                <div class="tag">
                    ${ext}
                    <button onclick="removeIncludeExtension('${ext}')">×</button>
                </div>
            `;
        });

        // Populate defaults
        document.getElementById('defaultOutputFolder').value = settings.defaultOutputFolder || '';
        document.getElementById('defaultMaxFileSizeMb').value = settings.defaultMaxFileSizeMb || '';

    } catch (error) {
        showMessage('Error loading settings: ' + error.message, 'error');
    }
}

async function addExcludePattern() {
    const pattern = document.getElementById('newExcludePattern').value.trim();
    if (!pattern) {
        showMessage('Please enter a pattern', 'error');
        return;
    }

    try {
        const response = await fetch('/api/settings/exclude-patterns', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pattern })
        });

        if (response.ok) {
            document.getElementById('newExcludePattern').value = '';
            showMessage('Pattern added successfully', 'success');
            loadSettings();
        } else {
            showMessage('Failed to add pattern', 'error');
        }
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function removeExcludePattern(pattern) {
    try {
        const response = await fetch('/api/settings/exclude-patterns/' + encodeURIComponent(pattern), {
            method: 'DELETE'
        });

        if (response.ok) {
            showMessage('Pattern removed successfully', 'success');
            loadSettings();
        } else {
            showMessage('Failed to remove pattern', 'error');
        }
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function addIncludeExtension() {
    const extension = document.getElementById('newIncludeExtension').value.trim();
    if (!extension) {
        showMessage('Please enter an extension', 'error');
        return;
    }

    try {
        const response = await fetch('/api/settings/include-extensions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ extension })
        });

        if (response.ok) {
            document.getElementById('newIncludeExtension').value = '';
            showMessage('Extension added successfully', 'success');
            loadSettings();
        } else {
            showMessage('Failed to add extension', 'error');
        }
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function removeIncludeExtension(ext) {
    try {
        const response = await fetch('/api/settings/include-extensions/' + encodeURIComponent(ext), {
            method: 'DELETE'
        });

        if (response.ok) {
            showMessage('Extension removed successfully', 'success');
            loadSettings();
        } else {
            showMessage('Failed to remove extension', 'error');
        }
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function updateDefaults() {
    const outputFolder = document.getElementById('defaultOutputFolder').value.trim();
    const maxFileSizeMb = parseInt(document.getElementById('defaultMaxFileSizeMb').value);

    try {
        const response = await fetch('/api/settings/defaults', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ outputFolder, maxFileSizeMb })
        });

        if (response.ok) {
            showMessage('Defaults updated successfully', 'success');
        } else {
            showMessage('Failed to update defaults', 'error');
        }
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function resetSettings() {
    if (!confirm('Are you sure you want to reset all settings to defaults?')) {
        return;
    }

    try {
        const response = await fetch('/api/settings/reset', {
            method: 'POST'
        });

        if (response.ok) {
            showMessage('Settings reset successfully', 'success');
            loadSettings();
        } else {
            showMessage('Failed to reset settings', 'error');
        }
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function showMessage(text, type) {
    const message = document.getElementById('message');
    message.textContent = text;
    message.className = 'message ' + type;
    message.style.display = 'block';

    setTimeout(() => {
        message.style.display = 'none';
    }, 5000);

}

// Back to Top Logic
const backToTopBtn = document.getElementById('backToTop');

window.onscroll = function () {
    if (document.body.scrollTop > 300 || document.documentElement.scrollTop > 300) {
        backToTopBtn.style.display = "flex";
    } else {
        backToTopBtn.style.display = "none";
    }
};

backToTopBtn.onclick = function () {
    window.scrollTo({ top: 0, behavior: 'smooth' });
};
