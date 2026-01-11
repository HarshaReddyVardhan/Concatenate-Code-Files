package com.concatenator.conatenate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class ConatenateApplication {

	private static final Logger log = LoggerFactory.getLogger(ConatenateApplication.class);
	private static final long startTime = System.currentTimeMillis();

	public static void main(String[] args) {
		// Disable headless mode to allow JFileChooser to work
		System.setProperty("java.awt.headless", "false");

		// Log startup info
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		String startTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		log.info("=================================================");
		log.info("Application starting...");
		log.info("PID: {}", pid);
		log.info("Start Time: {}", startTimestamp);
		log.info("Java Version: {}", System.getProperty("java.version"));
		log.info("Headless Mode: {}", System.getProperty("java.awt.headless"));
		log.info("=================================================");

		// Register shutdown hook to log when application stops
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			long uptime = System.currentTimeMillis() - startTime;
			String uptimeStr = formatUptime(uptime);
			log.info("=================================================");
			log.info("Application stopping...");
			log.info("PID: {}", pid);
			log.info("Shutdown Time: {}",
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			log.info("Total Uptime: {}", uptimeStr);
			log.info("=================================================");
		}, "ShutdownHook-Logging"));

		SpringApplication app = new SpringApplication(ConatenateApplication.class);
		app.setHeadless(false); // Ensure Spring doesn't override our headless setting
		app.run(args);
	}

	private static String formatUptime(long millis) {
		long seconds = millis / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		if (days > 0) {
			return String.format("%dd %dh %dm %ds", days, hours % 24, minutes % 60, seconds % 60);
		} else if (hours > 0) {
			return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
		} else if (minutes > 0) {
			return String.format("%dm %ds", minutes, seconds % 60);
		} else {
			return String.format("%ds", seconds);
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void openBrowser() {
		String url = "http://localhost:8080";
		String isDocker = System.getenv("APP_IN_DOCKER");

		log.info("=================================================");
		log.info("Application ready at: " + url);
		log.info("=================================================");

		// Skip auto-open in Docker (no GUI available)
		if ("true".equalsIgnoreCase(isDocker)) {
			log.info("Running in Docker - Open manually: " + url);
			return; // ← Only skip the Desktop stuff
		}

		// On local machine, try to open browser
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(url));
				log.info("Browser opened successfully");
			}
		} catch (Exception e) {
			log.error("Could not open browser: " + e.getMessage());
		}
	}

}
