package com.concatenator.conatenate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class FileHashService {

    /**
     * Calculate SHA-256 hash for a file.
     * This is used to detect if a file has changed since the last scan.
     *
     * How it works:
     * 1. Reads file content as bytes
     * 2. Passes bytes through SHA-256 algorithm
     * 3. Converts hash bytes to hexadecimal string
     *
     * @param filePath - Path to the file
     * @return SHA-256 hash as hexadecimal string (e.g., "a3f5b2c1...")
     * @throws IOException if file cannot be read
     */
    public String calculateFileHash(Path filePath) throws IOException {
        try {
            // Get SHA-256 algorithm instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Read all file bytes and compute hash
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            // Convert hash bytes to hexadecimal string
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            // This should never happen - SHA-256 is always available
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convert byte array to hexadecimal string.
     * Example: [255, 0, 128] → "ff0080"
     *
     * @param bytes - Byte array to convert
     * @return Hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            // Convert each byte to 2-character hex string
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0'); // Add leading zero if needed
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
