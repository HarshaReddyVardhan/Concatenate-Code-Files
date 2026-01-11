package com.concatenator.conatenate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/utils")
public class UtilsController {

    private static final Logger log = LoggerFactory.getLogger(UtilsController.class);

    @jakarta.annotation.PostConstruct
    public void init() {
        // "Warm up" Swing in a separate thread to avoid blocking startup
        new Thread(() -> {
            try {
                if (!GraphicsEnvironment.isHeadless()) {
                    log.info("Initializing Swing components for faster first-time load...");
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    // Force class loading
                    new JFileChooser();
                    log.info("Swing initialization complete.");
                }
            } catch (Exception e) {
                log.warn("Failed to pre-initialize Swing: {}", e.getMessage());
            }
        }).start();
    }

    @GetMapping("/env-info")
    public ResponseEntity<Map<String, Object>> getEnvInfo() {
        boolean isDocker = "true".equalsIgnoreCase(System.getenv("APP_IN_DOCKER"));
        boolean isHeadless = GraphicsEnvironment.isHeadless();

        return ResponseEntity.ok(Map.of(
                "isDocker", isDocker,
                "isHeadless", isHeadless));
    }

    @GetMapping("/browse-folder")
    public ResponseEntity<Map<String, String>> browseFolder() {
        long startTime = System.currentTimeMillis();
        log.info("=== Browse Folder Request Started ===");
        log.info("Thread: {}", Thread.currentThread().getName());

        final String[] selectedPath = { null };
        final String[] errorMessage = { null };

        try {
            // Check if we are in headless mode
            if (GraphicsEnvironment.isHeadless()) {
                log.error("[HEADLESS_ERROR] Cannot open file chooser: Server is running in headless mode");
                log.error("java.awt.headless system property: {}", System.getProperty("java.awt.headless"));
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error",
                                "Server is running in headless mode (e.g. Docker). Cannot open file chooser."));
            }

            log.info("Headless check passed - java.awt.headless={}", System.getProperty("java.awt.headless"));

            SwingUtilities.invokeAndWait(() -> {
                try {
                    // Look and feel is already set in init(), but safe to call again or check
                    if (!UIManager.getLookAndFeel().getClass().getName()
                            .equals(UIManager.getSystemLookAndFeelClassName())) {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }
                } catch (Exception e) {
                    log.warn("[UI_WARNING] Could not set system look and feel: {}", e.getMessage());
                }

                log.info("Creating JFileChooser dialog...");
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select Project Folder");

                // Set default directory
                String currentPath = System.getProperty("user.home");
                chooser.setCurrentDirectory(new File(currentPath));
                log.info("File chooser starting directory: {}", currentPath);

                // Create a temporary parent frame with robust focus handling
                log.debug("Creating parent frame for dialog...");
                JFrame frame = new JFrame();
                frame.setUndecorated(true);
                frame.setSize(0, 0);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.setAlwaysOnTop(true);
                frame.toFront();
                frame.requestFocus();
                log.info("Parent frame created with focus - alwaysOnTop: true");

                log.info(">>> Opening folder selection dialog (waiting for user interaction)...");
                int result = chooser.showOpenDialog(frame);
                log.info("<<< Dialog closed with result code: {}", result);

                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedPath[0] = chooser.getSelectedFile().getAbsolutePath();
                    log.info("[SUCCESS] User selected folder: {}", selectedPath[0]);
                } else if (result == JFileChooser.CANCEL_OPTION) {
                    log.info("[CANCELLED] User cancelled folder selection");
                } else {
                    log.warn("[UNKNOWN] Dialog returned unexpected result: {}", result);
                }

                frame.dispose();
                log.debug("Parent frame disposed");
            });

        } catch (java.lang.reflect.InvocationTargetException e) {
            log.error("[INVOCATION_ERROR] InvocationTargetException while opening file chooser");
            log.error("Root cause: {}", e.getCause() != null ? e.getCause().getClass().getName() : "null");
            log.error("Stack trace:", e);
            errorMessage[0] = "Failed to open file chooser: " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        } catch (InterruptedException e) {
            log.error("[INTERRUPTED] Thread interrupted while waiting for file chooser", e);
            Thread.currentThread().interrupt();
            errorMessage[0] = "File chooser was interrupted";
        } catch (Exception e) {
            log.error("[UNEXPECTED_ERROR] Unexpected error opening file chooser: {}", e.getClass().getName());
            log.error("Stack trace:", e);
            errorMessage[0] = "Failed to open file chooser: " + e.getMessage();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("=== Browse Folder Request Completed in {}ms ===", duration);

        if (errorMessage[0] != null) {
            log.error("Returning error response: {}", errorMessage[0]);
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", errorMessage[0]));
        }

        if (selectedPath[0] != null) {
            log.info("Returning selected path: {}", selectedPath[0]);
            return ResponseEntity.ok(Collections.singletonMap("path", selectedPath[0]));
        } else {
            log.info("Returning empty response (user cancelled)");
            return ResponseEntity.ok(Collections.emptyMap());
        }
    }

    @GetMapping("/list-dirs")
    public ResponseEntity<List<Map<String, Object>>> listDirectories(@RequestParam(required = false) String path) {
        List<Map<String, Object>> result = new ArrayList<>();
        File[] files;

        if (path == null || path.trim().isEmpty()) {
            files = File.listRoots();
        } else {
            files = new File(path).listFiles();
        }

        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && !f.isHidden()) {
                    result.add(Map.of(
                            "name", f.getName().isEmpty() ? f.getAbsolutePath() : f.getName(),
                            "path", f.getAbsolutePath()));
                }
            }
        }

        result.sort(Comparator.comparing(m -> ((String) m.get("name")).toLowerCase()));

        return ResponseEntity.ok(result);
    }
}
