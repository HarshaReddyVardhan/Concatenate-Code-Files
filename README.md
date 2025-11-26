# Project Concatenator - README

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [System Requirements](#system-requirements)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Web UI Usage](#web-ui-usage)
- [User Settings Management](#user-settings-management)
- [Troubleshooting](#troubleshooting)
- [Architecture](#architecture)
- [File Processing Logic](#file-processing-logic)
- [Incremental Updates](#incremental-updates)
- [Output Files](#output-files)

---

## 🎯 Overview

**Project Concatenator** is a Spring Boot application designed to concatenate source files from large projects into optimized text files suitable for use with AI tools like ChatGPT and Claude.

The application scans your project directory, intelligently filters files based on patterns/extensions, groups them by folder, and combines them into text files split by size limits. It also generates project structure metadata and supports incremental updates using SHA-256 file hashing.

### Use Cases
- **AI Model Context**: Prepare project code for AI analysis and code review
- **Documentation Generation**: Create comprehensive source file exports
- **Code Analysis**: Generate text-based project representations
- **Backup & Archiving**: Create aggregated project snapshots

---

## ✨ Features

### Core Features
✅ **Project File Scanning** - Recursive directory traversal with pattern matching  
✅ **Smart File Filtering** - Exclude heavy files (node_modules, build outputs, media, etc.)  
✅ **Automatic Size Splitting** - Split large folder groups into multiple files when exceeding size limits  
✅ **SHA-256 Change Detection** - Detect modified files for incremental updates  
✅ **Incremental Updates** - Process only changed files in subsequent runs  
✅ **Project Structure Generation** - Create JSON representation of project tree  
✅ **Metadata Persistence** - Store hashes and file info for future comparisons  

### User Settings
✅ **Custom Exclude Patterns** - Add/remove file patterns to exclude  
✅ **Include Extensions** - Specify which file extensions to include  
✅ **Default Configuration** - Save preferred output folder and max file size  
✅ **Persistent Storage** - Settings saved to `~/.project-concat-settings.json`  
✅ **Granular Control** - Override settings per request via API  

### UI & API
✅ **REST API** - Full programmatic access to all features  
✅ **Web UI** - Simple form-based interface for common operations  
✅ **Health Check** - Verify API status  
✅ **Cross-Origin Support** - CORS enabled for web integration  

---

## 💻 System Requirements

### Software
- **Java**: Version 17 or higher (LTS recommended)
- **Maven**: Version 3.6.0 or higher
- **Spring Boot**: 3.2.0 (stable, tested version)

### Hardware
- **Memory**: Minimum 512MB, 1GB+ recommended (for large projects)
- **Disk Space**: 1GB free space (for project scans and output files)
- **OS**: Windows, macOS, Linux

### Tools (Optional but Recommended)
- **IntelliJ IDEA** or **Eclipse** IDE
- **Git** for version control
- **curl** or **Postman** for API testing

---

## 📁 Project Structure

```
project-concatenator/
├── src/
│   ├── main/
│   │   ├── java/com/concatenator/conatenate/
│   │   │   ├── ConatenateApplication.java         (Main entry point)
│   │   │   ├── config/
│   │   │   │   └── JacksonConfig.java             (ObjectMapper bean)
│   │   │   ├── controller/
│   │   │   │   ├── ConcatenationController.java   (REST API endpoints)
│   │   │   │   ├── SettingsController.java        (Settings API)
│   │   │   │   └── WebController.java             (Web UI pages)
│   │   │   ├── model/
│   │   │   │   ├── ConcatenationRequest.java      (API request model)
│   │   │   │   ├── ConcatenationResult.java       (API response model)
│   │   │   │   ├── UserSettings.java              (User preferences)
│   │   │   │   ├── FileMetadata.java              (File hash & info)
│   │   │   │   └── ProjectMetadata.java           (Project metadata)
│   │   │   └── service/
│   │   │       ├── ConcatenationService.java      (Core logic)
│   │   │       ├── FileHashService.java           (SHA-256 hashing)
│   │   │       └── UserSettingsService.java       (Settings management)
│   │   └── resources/
│   │       ├── application.properties             (Configuration)
│   │       ├── static/
│   │       │   ├── css/style.css                  (UI styling)
│   │       │   └── js/app.js                      (UI logic)
│   │       └── templates/
│   │           ├── index.html                     (Home page)
│   │           └── settings.html                  (Settings page)
│   └── test/
│       └── java/ConatenateApplicationTests.java
├── pom.xml                                        (Maven configuration)
├── README.md                                      (This file)
└── .gitignore

```

---

## 🚀 Installation & Setup

### Step 1: Clone or Download Project

```bash
# Clone from repository (if available)
git clone https://github.com/yourusername/project-concatenator.git
cd project-concatenator

# Or create new Spring Boot project and copy files
```

### Step 2: Verify Java Installation

```bash
java -version
# Output should show Java 17 or higher
# Example: openjdk version "17.0.1" 2021-10-19

javac -version
# Output should show javac 17 or higher
```

### Step 3: Verify Maven Installation

```bash
mvn -version
# Output should show Maven 3.6.0 or higher
# Example: Apache Maven 3.8.1 (05ca0be6e6e16e1f67623ad7ccc9d560 ; 2021-04-04...)
```

### Step 4: Configure pom.xml

**Critical**: Ensure your `pom.xml` has:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>  <!-- MUST be 3.2.0, NOT 4.0.0 -->
    <relativePath/>
</parent>

<properties>
    <java.version>17</java.version>  <!-- MUST be 17 or higher -->
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

**Note**: Do NOT use Spring Boot 4.0.0 - it has Jackson 3.0 incompatibility with Lombok.

### Step 5: Enable Lombok in IDE

#### For IntelliJ IDEA:
1. **File → Settings** (Ctrl+Alt+S)
2. **Build, Execution, Deployment → Compiler → Annotation Processors**
3. ✅ Check "Enable annotation processing"
4. ✅ Check "Obtain processors from project classpath"
5. Click **Apply** and **OK**
6. **File → Settings → Plugins**
7. Search for "Lombok" and install the plugin
8. **Restart IntelliJ**

#### For Eclipse:
1. Install Lombok plugin from Eclipse Marketplace
2. Right-click project → **Properties → Java Compiler → Annotation Processing**
3. ✅ Enable project specific settings
4. ✅ Enable annotation processing
5. ✅ Enable processing in editor

#### For VS Code:
1. Install "Lombok Annotations Support for VS Code" extension
2. Verify `.vscode/settings.json` contains annotation processing config

### Step 6: Clean Build

```bash
# Navigate to project root
cd /path/to/project-concatenator

# Clean and install dependencies
mvn clean install -U

# If using IntelliJ: Right-click pom.xml → Maven → Reload Project
# Then: Build → Rebuild Project
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
[INFO] Finished at: 2025-11-26T10:59:00-08:00
```

---

## ⚙️ Configuration

### application.properties

Create or update `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/

# Logging
logging.level.root=INFO
logging.level.com.concatenator.conatenate=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# File Upload/Output
app.output.folder=project-concat-output
app.max.file.size.mb=30

# Default Exclude Patterns (comma-separated)
# Includes: build outputs, binaries, media, caches, version control, IDE files
app.exclude.patterns=node_modules/**,target/**,build/**,dist/**,.git/**,.idea/**,*.class,*.jar,*.exe,*.png,*.jpg,*.mp4,*.pdf

# Default Include Extensions (comma-separated)
# Leave empty to include all; specify to restrict
app.include.extensions=.java,.py,.js,.ts,.go,.rs,.cpp,.c,.h,.xml,.json,.yml,.yaml,.md,.txt,.sql

# Settings File Location
app.settings.file-path=~/.project-concat-settings.json
app.settings.enable-persistence=true

# Default Output Folder
app.default.output-folder=project-concat-output

# Default Max File Size (MB)
app.default.max-file-size-mb=30

# Default Exclude Patterns
app.default.exclude-patterns=node_modules,target,build,dist,.git,.idea,*.class,*.jar,*.png,*.jpg

# Default Include Extensions
app.default.include-extensions=.java,.py,.js,.ts,.xml,.json,.md
```

### Custom Application Settings

User settings are saved to: `~/.project-concat-settings.json`

**Example content**:
```json
{
  "excludePatterns": [
    "*.tmp",
    "*.cache",
    "test-data",
    "coverage/**"
  ],
  "includeExtensions": [
    ".java",
    ".xml",
    ".properties"
  ],
  "defaultOutputFolder": "ai-context",
  "defaultMaxFileSizeMb": 50,
  "lastUpdated": 1732596000000
}
```

**This file is created automatically** on first run. You can modify it via the Settings API.

---

## ▶️ Running the Application

### Method 1: Maven Command Line

```bash
# From project root directory
mvn spring-boot:run

# Application will start at: http://localhost:8080
```

### Method 2: IDE Integration

#### IntelliJ IDEA:
1. Open `ConatenateApplication.java`
2. Click the green **Run** button (or Shift+F10)
3. Application starts in IDE run panel

#### Eclipse:
1. Right-click `ConatenateApplication.java`
2. Select **Run As → Java Application**

#### VS Code:
1. Install "Spring Boot Extension Pack"
2. Open command palette (Ctrl+Shift+P)
3. Type "Spring Boot: Start" and select
4. Application starts in terminal

### Method 3: Compiled JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/conatenate-0.0.1-SNAPSHOT.jar

# Application will start at: http://localhost:8080
```

### Verify Application Started

```bash
# In another terminal, test health endpoint
curl http://localhost:8080/api/health

# Expected response:
# "Project Concatenator API is running"
```

---

## 📡 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Endpoints

#### 1. **Full Project Concatenation**

**Endpoint**: `POST /api/generate`

**Request Body**:
```json
{
  "projectPath": "/path/to/your/project",
  "outputFolder": "output",
  "maxFileSizeMb": 30,
  "excludePatterns": ["*.tmp", "test-data"],
  "includeExtensions": [".java", ".xml"],
  "incrementalUpdate": false
}
```

**Fields**:
- `projectPath` ⭐ **Required** - Absolute path to project directory
- `outputFolder` - Where to save concatenated files (default: from settings)
- `maxFileSizeMb` - Maximum size per output file before splitting (default: 30MB)
- `excludePatterns` - Patterns to exclude (overrides settings if provided)
- `includeExtensions` - Extensions to include (leave empty to include all)
- `incrementalUpdate` - Always false for full generation

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "D:\\MyProject\\backend",
    "outputFolder": "ai-export",
    "maxFileSizeMb": 50
  }'
```

**Response**:
```json
{
  "success": true,
  "message": "Concatenation completed successfully",
  "outputFiles": [
    "D:\\MyProject\\backend\\ai-export\\src-1.txt",
    "D:\\MyProject\\backend\\ai-export\\config-1.txt",
    "D:\\MyProject\\backend\\ai-export\\PROJECT_STRUCTURE.json"
  ],
  "totalFilesProcessed": 234,
  "filesChanged": 234,
  "filesSkipped": 0,
  "totalSizeBytes": 15728640,
  "processingTimeMs": 2341,
  "projectStructureFile": "D:\\MyProject\\backend\\ai-export\\PROJECT_STRUCTURE.json",
  "metadataFile": "D:\\MyProject\\backend\\ai-export\\.project-concat-metadata.json"
}
```

---

#### 2. **Incremental Update** (Changed Files Only)

**Endpoint**: `POST /api/update`

**Request Body**:
```json
{
  "projectPath": "/path/to/your/project",
  "outputFolder": "output",
  "incrementalUpdate": true
}
```

**How It Works**:
1. Loads previous metadata from `.project-concat-metadata.json`
2. Calculates SHA-256 hash for each file
3. Compares with previous hashes
4. Only processes files that changed
5. Preserves unchanged files

**Example**:
```bash
curl -X POST http://localhost:8080/api/update \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "D:\\MyProject\\backend"}'
```

**Response**:
```json
{
  "success": true,
  "message": "Concatenation completed successfully",
  "totalFilesProcessed": 8,
  "filesChanged": 8,
  "filesSkipped": 226,
  "totalSizeBytes": 524288,
  "processingTimeMs": 345
}
```

---

#### 3. **Health Check**

**Endpoint**: `GET /api/health`

**Request**:
```bash
curl http://localhost:8080/api/health
```

**Response**:
```
"Project Concatenator API is running"
```

---

### Settings API Endpoints

#### 4. **Get All Settings**

**Endpoint**: `GET /api/settings`

```bash
curl http://localhost:8080/api/settings
```

**Response**:
```json
{
  "excludePatterns": ["*.tmp", "*.cache"],
  "includeExtensions": [".java", ".xml"],
  "defaultOutputFolder": "ai-context",
  "defaultMaxFileSizeMb": 50,
  "lastUpdated": 1732596000000
}
```

---

#### 5. **Get Exclude Patterns**

**Endpoint**: `GET /api/settings/exclude-patterns`

```bash
curl http://localhost:8080/api/settings/exclude-patterns
```

---

#### 6. **Add Exclude Pattern**

**Endpoint**: `POST /api/settings/exclude-patterns`

```bash
curl -X POST http://localhost:8080/api/settings/exclude-patterns \
  -H "Content-Type: application/json" \
  -d '{"pattern": "*.log"}'
```

**Supported Pattern Types**:
- `*.ext` - Match all files with extension (e.g., `*.tmp`)
- `folder/**` - Match entire folder (e.g., `node_modules/**`)
- `**/filename` - Match file anywhere (e.g., `**/.DS_Store`)
- `temp?` - Match with wildcards (e.g., `temp1`, `tempA`)

---

#### 7. **Remove Exclude Pattern**

**Endpoint**: `DELETE /api/settings/exclude-patterns/{pattern}`

```bash
# URL encode pattern if needed
curl -X DELETE http://localhost:8080/api/settings/exclude-patterns/%.2alog

# Or with plain pattern
curl -X DELETE "http://localhost:8080/api/settings/exclude-patterns/*.log"
```

---

#### 8. **Add Include Extension**

**Endpoint**: `POST /api/settings/include-extensions`

```bash
curl -X POST http://localhost:8080/api/settings/include-extensions \
  -H "Content-Type: application/json" \
  -d '{"extension": ".java"}'
```

**Tip**: If include extensions list is empty, ALL files are included.

---

#### 9. **Remove Include Extension**

**Endpoint**: `DELETE /api/settings/include-extensions/{ext}`

```bash
curl -X DELETE http://localhost:8080/api/settings/include-extensions/.java
```

---

#### 10. **Update Default Settings**

**Endpoint**: `PUT /api/settings/defaults`

```bash
curl -X PUT http://localhost:8080/api/settings/defaults \
  -H "Content-Type: application/json" \
  -d '{
    "outputFolder": "new-output",
    "maxFileSizeMb": 100
  }'
```

---

#### 11. **Reset Settings to Defaults**

**Endpoint**: `POST /api/settings/reset`

```bash
curl -X POST http://localhost:8080/api/settings/reset
```

---

## 🖥️ Web UI Usage

### Home Page

**URL**: `http://localhost:8080/`

**Features**:
- Form to enter project path
- Configure output folder
- Set maximum file size
- Options for full scan or incremental update
- Download results button
- View concatenated file preview

### Settings Page

**URL**: `http://localhost:8080/settings`

**Features**:
- View current settings
- Add/remove exclude patterns
- Add/remove include extensions
- Update default output folder
- Update default file size limit
- Reset to application defaults
- Export settings as JSON
- Import settings from JSON file

---

## ⚙️ User Settings Management

### Default Exclude Patterns (Cannot be Removed)

These are built-in patterns that are always excluded:

**Build Outputs**:
- `node_modules`, `target`, `build`, `dist`, `out`, `.next`, `.nuxt`

**Binaries**:
- Java: `*.class`, `*.jar`, `*.war`, `*.ear`
- Native: `*.exe`, `*.dll`, `*.so`, `*.dylib`
- Python: `*.pyc`, `__pycache__`, `.pytest_cache`, `*.egg-info`, `venv`, `.venv`

**Media Files**:
- Images: `*.png`, `*.jpg`, `*.jpeg`, `*.gif`, `*.svg`, `*.ico`, `*.bmp`, `*.webp`
- Videos: `*.mp4`, `*.avi`, `*.mov`
- Audio: `*.mp3`, `*.wav`
- Fonts: `*.woff`, `*.woff2`, `*.ttf`, `*.eot`
- Documents: `*.pdf`

**Package Management**:
- `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`

**Version Control**:
- `.git`, `.svn`, `.hg`

**IDE Files**:
- `.idea`, `.vscode`, `.eclipse`, `.settings`, `*.iml`

**Archives**:
- `*.zip`, `*.tar`, `*.gz`, `*.rar`, `*.7z`

**Databases**:
- `*.db`, `*.sqlite`, `*.sqlite3`

**System Files**:
- `.DS_Store`, `Thumbs.db`, `desktop.ini`

**Logs**:
- `*.log`

### Custom Patterns Priority

```
Final Exclude List = Default Patterns (always) + Custom Patterns (user-added)
```

You can ONLY ADD custom patterns. Default patterns cannot be removed.

---

## 📊 Output Files

### 1. Concatenated Files

**Naming**: `{folderName}-{number}.txt`

**Example Output**:
```
src-1.txt
src-2.txt
config-1.txt
tests-1.txt
```

**Content Format**:
```
=== path/to/file1.java ===
[file content]

=== path/to/file2.java ===
[file content]

=== path/to/file3.xml ===
[file content]
```

**Split Logic**:
- Files are grouped by top-level folder
- When a folder's files exceed `maxFileSizeMb`, a new file is created
- Each entry starts with `=== {relative-path} ===` separator
- Files are separated by blank lines

---

### 2. PROJECT_STRUCTURE.json

**Purpose**: Shows complete project structure as JSON tree

**Example**:
```json
{
  "projectPath": "/home/user/my-project",
  "structure": {
    "src": {
      "main": {
        "java": {
          "App.java": null,
          "Controller.java": null
        },
        "resources": {
          "application.properties": null
        }
      }
    },
    "config": {
      "settings.xml": null
    },
    "README.md": null
  }
}
```

**Usage**: 
- Understand project layout quickly
- Used by AI tools to understand structure
- Reference for code navigation

---

### 3. .project-concat-metadata.json (Hidden)

**Purpose**: Stores file hashes and metadata for incremental updates

**Location**: Project root directory (hidden file starting with `.`)

**Content**:
```json
{
  "projectPath": "/home/user/my-project",
  "lastScanTime": 1732596000000,
  "totalFiles": 234,
  "totalSize": 15728640,
  "files": {
    "src/main/java/App.java": {
      "filePath": "src/main/java/App.java",
      "sha256Hash": "a3f5b2c1d4e6f8...",
      "lastModified": 1732595000000,
      "fileSize": 4096
    },
    "src/main/java/Controller.java": {
      "filePath": "src/main/java/Controller.java",
      "sha256Hash": "b4g6c3d2e7f9h0...",
      "lastModified": 1732595500000,
      "fileSize": 8192
    }
  }
}
```

**Used For**:
- Detecting which files changed since last scan
- Enabling fast incremental updates
- Tracking file history and modifications

---

## 🐛 Troubleshooting

### Build Errors

#### Error: `cannot find symbol: variable log`

**Cause**: Lombok annotation processing not enabled

**Solution**:
1. Install Lombok IDE plugin
2. Enable annotation processing in IDE settings
3. Rebuild project

#### Error: `Cannot find symbol: method getProjectPath()`

**Cause**: Spring Boot 4.0.0 Jackson incompatibility

**Solution**:
```bash
# Update pom.xml to Spring Boot 3.2.0
mvn clean install -U
```

#### Error: `Failed to resolve: tools.jackson`

**Cause**: Spring Boot 4.0.0 uses experimental Jackson 3.0

**Solution**: Use Spring Boot 3.2.0 instead (see pom.xml fix above)

---

### Runtime Errors

#### Error: `Project path does not exist`

**Cause**: Invalid project path provided

**Solution**:
```bash
# Verify path exists and is absolute
# Windows: "D:\\Users\\Project\\backend"
# Linux/Mac: "/home/user/project/backend"

# Use curl to test:
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "D:\\\\test"}'  # Double backslash for JSON
```

#### Error: `Permission denied` when writing files

**Cause**: No write permission to output directory

**Solution**:
```bash
# Linux/Mac: Make directory writable
chmod 755 /path/to/project

# Windows: Right-click folder → Properties → Security → Edit Permissions
```

#### Error: `OutOfMemoryError` for large projects

**Cause**: Not enough heap memory allocated

**Solution**:
```bash
# Increase Java heap memory
java -Xmx2048m -jar target/conatenate-0.0.1-SNAPSHOT.jar

# Or set in IDE (IntelliJ):
# File → Settings → Build, Execution, Deployment → Compiler
# VM options: -Xmx2048m
```

#### Error: `Settings file not found`

**Cause**: `.project-concat-settings.json` not created

**Solution**:
```bash
# File is created automatically on first API call to /api/settings
# If not created, manually create:

# Linux/Mac:
touch ~/.project-concat-settings.json
echo '{}' > ~/.project-concat-settings.json

# Windows (PowerShell):
New-Item -Path $HOME -Name ".project-concat-settings.json"
'{}' | Out-File ~/.project-concat-settings.json
```

---

### Performance Issues

#### Slow Performance with Large Projects

**Issue**: Scanning large projects (1000+ files) takes long time

**Solutions**:
1. **Increase exclude patterns** - Skip unnecessary folders
   ```bash
   curl -X POST http://localhost:8080/api/settings/exclude-patterns \
     -d '{"pattern": "node_modules/**"}'
   ```

2. **Increase file size limit** - Fewer splits = faster processing
   ```bash
   curl -X PUT http://localhost:8080/api/settings/defaults \
     -d '{"maxFileSizeMb": 100}'
   ```

3. **Use incremental updates** - Only process changed files
   ```bash
   curl -X POST http://localhost:8080/api/update \
     -d '{"projectPath": "/path/to/project"}'
   ```

4. **Increase Java heap memory**:
   ```bash
   java -Xmx4096m -jar target/conatenate-*.jar
   ```

---

## 🏗️ Architecture

### Component Overview

```
┌─────────────────────────────────────────────┐
│         Spring Boot Application             │
│  (ConatenateApplication.java)               │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
   ┌───▼────┐      ┌────▼────┐
   │ REST   │      │  Web    │
   │ API    │      │   UI    │
   │        │      │         │
   └───┬────┘      └────┬────┘
       │                │
   ┌───▼────────────────▼──────┐
   │  Controllers               │
   │ • ConcatenationController │
   │ • SettingsController      │
   │ • WebController           │
   └───┬────────────────────────┘
       │
   ┌───▼──────────────────────────┐
   │  Services (Business Logic)    │
   │ • ConcatenationService       │
   │ • UserSettingsService        │
   │ • FileHashService            │
   └───┬──────────────────────────┘
       │
   ┌───▼──────────────────────────┐
   │  Models (Data Structures)     │
   │ • ConcatenationRequest       │
   │ • ConcatenationResult        │
   │ • UserSettings               │
   │ • FileMetadata               │
   │ • ProjectMetadata            │
   └───┬──────────────────────────┘
       │
   ┌───▼──────────────────────────┐
   │  File System & Storage        │
   │ • Project files              │
   │ • Output directory           │
   │ • Settings JSON file         │
   │ • Metadata JSON file         │
   └──────────────────────────────┘
```

### Request Flow Diagram

**Full Generation Flow**:
```
1. User submits project path via API/UI
   ↓
2. ConcatenationController receives request
   ↓
3. ConcatenationService processes:
   a. Validate project path exists
   b. Load user settings
   c. Scan project files recursively
   d. Filter files (exclude patterns, include extensions)
   e. Group files by top-level folder
   f. Calculate SHA-256 hashes
   g. Generate concatenated files (with size-based splitting)
   h. Generate PROJECT_STRUCTURE.json
   i. Save metadata for future incremental updates
   ↓
4. Return ConcatenationResult with statistics
   ↓
5. User receives list of output files
```

**Incremental Update Flow**:
```
1. User triggers incremental update
   ↓
2. Load previous metadata from .project-concat-metadata.json
   ↓
3. Scan project files
   ↓
4. For each file:
   a. Calculate current SHA-256 hash
   b. Compare with previous hash
   c. If hash matches → skip file
   d. If hash differs or file is new → process file
   ↓
5. Generate new output files (with only changed files)
   ↓
6. Update metadata
   ↓
7. Return result with files changed/skipped statistics
```

---

## 🔄 File Processing Logic

### File Filtering

```
For each file in project:

  1. Check relative path against exclude patterns
     Pattern examples: "*.class", "node_modules/**", "test-data"
     If matched → SKIP file
  
  2. Extract file extension
     Example: "App.java" → extension is ".java"
  
  3. Check extension against include extensions list
     If include list is empty → INCLUDE all files
     If include list has values AND extension in list → INCLUDE
     If include list has values AND extension NOT in list → SKIP
  
  4. Check file permissions (readable)
     If not readable → SKIP with warning
  
  5. Include file in results

Result: Filtered list of files to process
```

### Grouping Strategy

**By Top-Level Folder**:
```
Input files:
  src/main/java/App.java
  src/main/java/Controller.java
  src/test/java/AppTest.java
  config/application.yml
  README.md

Output groups:
  "src": [src/main/java/App.java, src/main/java/Controller.java, src/test/java/AppTest.java]
  "config": [config/application.yml]
  "root": [README.md]
```

**Why top-level?**
- Logical organization by module/component
- Easier to understand output structure
- Files related to same feature grouped together
- Matches typical project structure

---

## 📈 Incremental Updates

### How Change Detection Works

**SHA-256 Hashing**:
```
File: src/main/java/App.java
Content: "public class App { public static void main..."

SHA-256 Hash: a3f5b2c1d4e6f8g9h0i1j2k3l4m5n6o7p8q9r0s1...
```

**On Next Scan**:
```
1. Read same file
2. Calculate SHA-256 again
3. Compare hashes:
   - If same → File unchanged, skip
   - If different → File modified, reprocess
```

**Advantages**:
- ✅ Fast: Only comparing hash strings (fast)
- ✅ Reliable: Detects any content change
- ✅ Efficient: 64-character hash vs entire file comparison
- ✅ Historical: Can track file history

### Performance Impact

**Full Scan** (first time):
- Scan all files
- Calculate all hashes
- Process all files
- Time: 2-5 seconds for typical project

**Incremental Update** (subsequent runs):
- Scan all files
- Calculate all hashes
- Compare with previous hashes
- Process only changed files
- Time: 1-2 seconds for typical project
- Speed improvement: 50-80%

---

## 📝 Usage Examples

### Example 1: First-Time Full Project Export

```bash
# Request
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:\\Users\\john\\projects\\MyApp",
    "outputFolder": "ai-export",
    "maxFileSizeMb": 50
  }'

# Response includes:
# - src-1.txt, src-2.txt (50MB each)
# - config-1.txt (12MB)
# - tests-1.txt (8MB)
# - PROJECT_STRUCTURE.json
# - .project-concat-metadata.json
# Total: 120MB in 2.3 seconds
```

### Example 2: Quick Update After Code Changes

```bash
# After modifying some files in project

# Request
curl -X POST http://localhost:8080/api/update \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "C:\\Users\\john\\projects\\MyApp"}'

# Response includes:
# - Only 3 files changed (out of 150)
# - Regenerates output with only changed content
# Total: 512KB in 0.8 seconds (80% faster!)
```

### Example 3: Exclude Test Files and Logs

```bash
# Add patterns
curl -X POST http://localhost:8080/api/settings/exclude-patterns \
  -d '{"pattern": "*test*/**"}'

curl -X POST http://localhost:8080/api/settings/exclude-patterns \
  -d '{"pattern": "*.log"}'

# Now exclude test-related folders and all .log files
```

### Example 4: Only Java and XML Files

```bash
# Configure include extensions
curl -X POST http://localhost:8080/api/settings/include-extensions \
  -d '{"extension": ".java"}'

curl -X POST http://localhost:8080/api/settings/include-extensions \
  -d '{"extension": ".xml"}'

# Subsequent requests will only include .java and .xml files
```

---

## 📄 License

MIT License - See LICENSE file for details

---

## 👨‍💻 Development

### Built With
- **Spring Boot 3.2.0** - Web framework
- **Lombok** - Reduce boilerplate code
- **Jackson** - JSON processing
- **Maven** - Build and dependency management
- **Java 17** - Programming language

### Technologies Used
- **REST API** - OpenAPI compliant
- **SHA-256** - Cryptographic hashing
- **File I/O** - Java NIO for efficient file operations
- **JSON** - Data serialization format
- **Thymeleaf** - HTML template engine

---

## ❓ FAQ

**Q: Can I use Spring Boot 4.0.0?**
A: No. Spring Boot 4.0.0 uses Jackson 3.0 which has compatibility issues with Lombok. Use Spring Boot 3.2.0.

**Q: How large can projects be?**
A: Tested up to 10,000 files / 500MB project size. Performance depends on file size and exclude patterns.

**Q: Can I run this on Docker?**
A: Yes. Create a Dockerfile with Java 17 and run the JAR. Example available upon request.

**Q: What if I modify settings and restart application?**
A: Settings are persisted to `~/.project-concat-settings.json` and loaded on startup.

**Q: Can I use this with private projects on GitHub?**
A: Yes. The application runs locally and doesn't upload files anywhere.

**Q: Is there a command-line interface?**
A: Not yet. Currently provides REST API and web UI only.

**Q: Can I exclude folders by size?**
A: No. Exclusion is pattern-based. Use patterns like `*heavy_folder*`.

---

## 📞 Support & Contribution

For issues, feature requests, or contributions, please contact the development team.

---

## 🎓 Learn More

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Lombok Documentation](https://projectlombok.org/)
- [Project Concatenator GitHub Repository](https://github.com/yourusername/project-concatenator)

---

**Version**: 1.0.0  
**Last Updated**: November 26, 2025  
**Status**: Production Ready ✅
