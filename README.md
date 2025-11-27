# Project Concatenator

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17+-ED8B00?logo=java&logoColor=white)](https://www.java.com)
[![Spring Boot 3.2.0](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.6.0+-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)

## 📋 Overview

**Project Concatenator** is a Spring Boot application that concatenates source files from large projects into optimized text files suitable for use with AI tools like ChatGPT and Claude.

The application scans your project directory, intelligently filters files based on patterns/extensions, groups them by folder, and combines them into text files split by size limits. It also generates project structure metadata and supports incremental updates using SHA-256 file hashing.

### Use Cases
- **AI Model Context**: Prepare project code for AI analysis and code review
- **Documentation Generation**: Create comprehensive source file exports
- **Code Analysis**: Generate text-based project representations
- **Backup & Archiving**: Create aggregated project snapshots

---

## ✨ Features

✅ **Project File Scanning** - Recursive directory traversal with pattern matching  
✅ **Smart File Filtering** - Exclude heavy files (node_modules, build outputs, media, etc.)  
✅ **Automatic Size Splitting** - Split large folder groups into multiple files when exceeding size limits  
✅ **SHA-256 Change Detection** - Detect modified files for incremental updates  
✅ **Incremental Updates** - Process only changed files in subsequent runs  
✅ **Project Structure Generation** - Create JSON representation of project tree  
✅ **REST API** - Full programmatic access to all features  
✅ **Web UI** - Simple form-based interface for common operations  
✅ **User Settings** - Custom exclude patterns, include extensions, persistent configuration  
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

### Tools (Recommended)
- **IntelliJ IDEA** or **Eclipse** IDE
- **Git** for version control
- **curl** or **Postman** for API testing

---

## 🚀 Installation & Setup

### Step 1: Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/project-concatenator.git
cd project-concatenator
```

### Step 2: Verify Java & Maven

```bash
java -version
# Output: Java 17 or higher

mvn -version
# Output: Maven 3.6.0 or higher
```

### Step 3: Configure pom.xml

Ensure your `pom.xml` has the correct versions:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>  <!-- MUST be 3.2.0 -->
    <relativePath/>
</parent>

<properties>
    <java.version>17</java.version>  <!-- MUST be 17+ -->
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

**⚠️ Do NOT use Spring Boot 4.0.0** - it has Jackson 3.0 incompatibility with Lombok.

### Step 4: Enable Lombok in IDE

#### IntelliJ IDEA:
1. **File → Settings** (Ctrl+Alt+S)
2. **Build, Execution, Deployment → Compiler → Annotation Processors**
3. ✅ Check "Enable annotation processing"
4. ✅ Check "Obtain processors from project classpath"
5. **File → Settings → Plugins**
6. Install the **Lombok** plugin
7. **Restart IntelliJ**

#### Eclipse:
1. Install Lombok plugin from Eclipse Marketplace
2. Right-click project → **Properties → Java Compiler → Annotation Processing**
3. ✅ Enable project specific settings
4. ✅ Enable annotation processing

#### VS Code:
1. Install "Lombok Annotations Support for VS Code" extension
2. Verify `.vscode/settings.json` contains annotation processing config

### Step 5: Build Project

```bash
mvn clean install -U
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

---

## ▶️ Running the Application

### Option 1: Maven CLI

```bash
mvn spring-boot:run
# Application starts at: http://localhost:8080
```

### Option 2: IDE

**IntelliJ IDEA:**
- Open `ConatenateApplication.java`
- Click the green **Run** button (Shift+F10)

**Eclipse:**
- Right-click `ConatenateApplication.java` → **Run As → Java Application**

### Option 3: Compiled JAR

```bash
mvn clean package
java -jar target/conatenate-0.0.1-SNAPSHOT.jar
# Application starts at: http://localhost:8080
```

### Verify Application

```bash
curl http://localhost:8080/api/health
# Response: "Project Concatenator API is running"
```

---

## 📡 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Endpoints

#### 1. Full Project Concatenation

**POST** `/api/generate`

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "D:\\MyProject\\backend",
    "outputFolder": "ai-export",
    "maxFileSizeMb": 50
  }'
```

**Request Body**:
```json
{
  "projectPath": "/path/to/project",      // ⭐ Required
  "outputFolder": "output",                // Optional
  "maxFileSizeMb": 30,                    // Optional
  "excludePatterns": ["*.tmp"],           // Optional
  "includeExtensions": [".java"],         // Optional
  "incrementalUpdate": false              // Optional
}
```

**Response**:
```json
{
  "success": true,
  "message": "Concatenation completed successfully",
  "outputFiles": ["path/to/src-1.txt", "path/to/config-1.txt"],
  "totalFilesProcessed": 234,
  "filesChanged": 234,
  "filesSkipped": 0,
  "totalSizeBytes": 15728640,
  "processingTimeMs": 2341,
  "projectStructureFile": "path/to/PROJECT_STRUCTURE.json",
  "metadataFile": "path/to/.project-concat-metadata.json"
}
```

---

#### 2. Incremental Update

**POST** `/api/update`

```bash
curl -X POST http://localhost:8080/api/update \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "D:\\MyProject\\backend"}'
```

**Features:**
- Loads previous metadata from `.project-concat-metadata.json`
- Calculates SHA-256 hash for each file
- Compares with previous hashes
- Only processes files that changed
- 50-80% faster than full scan

**Response**:
```json
{
  "success": true,
  "totalFilesProcessed": 8,
  "filesChanged": 8,
  "filesSkipped": 226,
  "totalSizeBytes": 524288,
  "processingTimeMs": 345
}
```

---

#### 3. Health Check

**GET** `/api/health`

```bash
curl http://localhost:8080/api/health
```

---

### Settings API

#### Get All Settings

**GET** `/api/settings`

```bash
curl http://localhost:8080/api/settings
```

---

#### Add Exclude Pattern

**POST** `/api/settings/exclude-patterns`

```bash
curl -X POST http://localhost:8080/api/settings/exclude-patterns \
  -H "Content-Type: application/json" \
  -d '{"pattern": "*.log"}'
```

**Supported Patterns:**
- `*.ext` - Match extension (e.g., `*.tmp`)
- `folder/**` - Match entire folder (e.g., `node_modules/**`)
- `**/filename` - Match file anywhere (e.g., `**/.DS_Store`)
- `temp?` - Wildcard matching (e.g., `temp1`, `tempA`)

---

#### Remove Exclude Pattern

**DELETE** `/api/settings/exclude-patterns/{pattern}`

```bash
curl -X DELETE "http://localhost:8080/api/settings/exclude-patterns/*.log"
```

---

#### Add Include Extension

**POST** `/api/settings/include-extensions`

```bash
curl -X POST http://localhost:8080/api/settings/include-extensions \
  -H "Content-Type: application/json" \
  -d '{"extension": ".java"}'
```

---

#### Remove Include Extension

**DELETE** `/api/settings/include-extensions/{ext}`

```bash
curl -X DELETE http://localhost:8080/api/settings/include-extensions/.java
```

---

#### Update Default Settings

**PUT** `/api/settings/defaults`

```bash
curl -X PUT http://localhost:8080/api/settings/defaults \
  -H "Content-Type: application/json" \
  -d '{"outputFolder": "new-output", "maxFileSizeMb": 100}'
```

---

#### Reset Settings

**POST** `/api/settings/reset`

```bash
curl -X POST http://localhost:8080/api/settings/reset
```

---

## 🖥️ Web UI

### Home Page
**URL:** `http://localhost:8080/`

- Enter project path
- Configure output folder
- Set maximum file size
- Full scan or incremental update options
- Download results

### Settings Page
**URL:** `http://localhost:8080/settings`

- View current settings
- Add/remove exclude patterns
- Add/remove include extensions
- Update defaults
- Reset to application defaults

---

## 📊 Output Files

### 1. Concatenated Files
**Naming:** `{folderName}-{number}.txt`

**Content Format:**
```
=== path/to/file1.java ===
[file content]

=== path/to/file2.java ===
[file content]
```

**Features:**
- Files grouped by top-level folder
- Automatically split when exceeding `maxFileSizeMb`
- Each file clearly separated with `=== {path} ===` header

---

### 2. PROJECT_STRUCTURE.json
Shows complete project structure as JSON tree:

```json
{
  "projectPath": "/path/to/project",
  "structure": {
    "src": {
      "main": {
        "java": {
          "App.java": null,
          "Controller.java": null
        }
      }
    },
    "config": {
      "settings.xml": null
    }
  }
}
```

---

### 3. .project-concat-metadata.json (Hidden)
Stores file hashes for incremental updates:

```json
{
  "projectPath": "/path/to/project",
  "lastScanTime": 1732596000000,
  "totalFiles": 234,
  "files": {
    "src/main/java/App.java": {
      "filePath": "src/main/java/App.java",
      "sha256Hash": "a3f5b2c1d4e6f8...",
      "lastModified": 1732595000000,
      "fileSize": 4096
    }
  }
}
```

---

## 🔄 How It Works

### Full Generation Flow

1. **Validate** project path exists
2. **Load** user settings
3. **Scan** project files recursively
4. **Filter** files (exclude patterns, include extensions)
5. **Group** files by top-level folder
6. **Calculate** SHA-256 hashes
7. **Generate** concatenated files (with size-based splitting)
8. **Create** PROJECT_STRUCTURE.json
9. **Save** metadata for incremental updates
10. **Return** statistics and output file paths

### Incremental Update Flow

1. **Load** previous metadata
2. **Scan** project files
3. **Calculate** current SHA-256 hashes
4. **Compare** with previous hashes
5. **Process** only changed files
6. **Generate** new output files
7. **Update** metadata
8. **Return** statistics (files changed/skipped)

---

## ⚙️ Configuration

### application.properties

Located at `src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# Logging
logging.level.root=INFO
logging.level.com.concatenator.conatenate=DEBUG

# Defaults
app.output.folder=project-concat-output
app.max.file.size.mb=30
app.exclude.patterns=node_modules/**,target/**,build/**,.git/**,.idea/**,*.class,*.jar,*.png,*.jpg
app.include.extensions=.java,.py,.js,.ts,.xml,.json,.md
```

### Settings File

User settings saved to: `~/.project-concat-settings.json`

---

## 🐛 Troubleshooting

### Build Errors

#### Error: `cannot find symbol: variable log`
**Solution:** Install Lombok IDE plugin and enable annotation processing

#### Error: `Cannot find symbol: method getProjectPath()`
**Solution:** Use Spring Boot 3.2.0 (not 4.0.0)

#### Error: `Failed to resolve: tools.jackson`
**Solution:** Update pom.xml to Spring Boot 3.2.0

---

### Runtime Errors

#### Error: `Project path does not exist`
**Solution:** Verify path is absolute and exists
```bash
# Windows: "D:\\Users\\Project\\backend"
# Linux/Mac: "/home/user/project/backend"
```

#### Error: `Permission denied`
**Solution:** Make directory writable
```bash
# Linux/Mac:
chmod 755 /path/to/project
```

#### Error: `OutOfMemoryError`
**Solution:** Increase Java heap memory
```bash
java -Xmx2048m -jar target/conatenate-0.0.1-SNAPSHOT.jar
```

---

## 📝 Usage Examples

### Example 1: First-Time Export

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:\\Users\\john\\projects\\MyApp",
    "outputFolder": "ai-export",
    "maxFileSizeMb": 50
  }'
```

**Output:**
- `src-1.txt`, `src-2.txt` (50MB each)
- `config-1.txt` (12MB)
- `tests-1.txt` (8MB)
- `PROJECT_STRUCTURE.json`
- `.project-concat-metadata.json`

---

### Example 2: Quick Incremental Update

```bash
curl -X POST http://localhost:8080/api/update \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "C:\\Users\\john\\projects\\MyApp"}'
```

**Output:**
- Only 3 files changed (out of 150)
- 512KB in 0.8 seconds (80% faster!)

---

### Example 3: Exclude Patterns

```bash
# Exclude test files
curl -X POST http://localhost:8080/api/settings/exclude-patterns \
  -d '{"pattern": "*test*/**"}'

# Exclude logs
curl -X POST http://localhost:8080/api/settings/exclude-patterns \
  -d '{"pattern": "*.log"}'
```

---

### Example 4: Specific Extensions Only

```bash
# Only .java files
curl -X POST http://localhost:8080/api/settings/include-extensions \
  -d '{"extension": ".java"}'

# Only .xml files
curl -X POST http://localhost:8080/api/settings/include-extensions \
  -d '{"extension": ".xml"}'
```

---

## ❓ FAQ

**Q: Can I use Spring Boot 4.0.0?**
A: No. Spring Boot 4.0.0 uses Jackson 3.0 which has compatibility issues with Lombok. Use 3.2.0.

**Q: How large can projects be?**
A: Tested up to 10,000 files / 500MB. Performance depends on file size and exclude patterns.

**Q: Can I run this on Docker?**
A: Yes. Create a Dockerfile with Java 17 and run the JAR.

**Q: Are settings persistent?**
A: Yes. Settings saved to `~/.project-concat-settings.json` and loaded on startup.

**Q: Is this private?**
A: Yes. The application runs locally and doesn't upload files anywhere.

**Q: Is there a CLI?**
A: Not yet. Currently REST API and web UI only.

---

## 📚 Technologies

- **Spring Boot 3.2.0** - Web framework
- **Lombok** - Reduce boilerplate
- **Jackson** - JSON processing
- **Maven** - Build management
- **Java 17** - Programming language
- **SHA-256** - Cryptographic hashing
- **Thymeleaf** - HTML templates

---

## 📄 License

MIT License - See [LICENSE](LICENSE) file for details

---

**Version:** 1.0.0  
**Last Updated:** November 26, 2025  
**Status:** Production Ready ✅
