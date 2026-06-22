package com.learn.projeto_learn.service.captcha;

import com.learn.projeto_learn.dto.captcha.CaptchaResponseDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final int DIFFICULTY         = 4;
    private static final int EXPIRATION_MINUTES = 10;

    private final SecureRandom rng     = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> store   = new ConcurrentHashMap<>();
    private final ScheduledExecutorService          cleaner = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        cleaner.scheduleAtFixedRate(this::evict, 5, 5, TimeUnit.MINUTES);
    }

    public CaptchaResponseDTO generate() {
        byte[] bytes = new byte[16];
        rng.nextBytes(bytes);
        String challenge = HexFormat.of().formatHex(bytes);
        String id        = UUID.randomUUID().toString();
        store.put(id, new Entry(challenge, DIFFICULTY, LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES)));
        return new CaptchaResponseDTO(id, challenge, DIFFICULTY);
    }

    public boolean validate(String id, String nonce) {
        Entry entry = store.remove(id);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) return false;
        if (nonce == null || nonce.isBlank()) return false;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input         = entry.challenge() + ":" + nonce;
            byte[] hash          = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hashHex       = HexFormat.of().formatHex(hash);
            return hashHex.startsWith("0".repeat(entry.difficulty()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível", e);
        }
    }

    private void evict() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record Entry(String challenge, int difficulty, LocalDateTime expiresAt) {}
}
