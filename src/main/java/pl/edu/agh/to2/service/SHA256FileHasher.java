package pl.edu.agh.to2.service;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class SHA256FileHasher implements FileHasher {
    @Override
    public String hash(String path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (FileInputStream fis = new FileInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
