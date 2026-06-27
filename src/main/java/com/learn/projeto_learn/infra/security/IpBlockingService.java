package com.learn.projeto_learn.infra.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IpBlockingService {

    private static final int MAX_FAILURES = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;

    private record AttemptInfo(int failures, LocalDateTime blockedUntil) {}

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null || info.blockedUntil() == null) return false;
        if (LocalDateTime.now().isAfter(info.blockedUntil())) {
            attempts.remove(ip);
            return false;
        }
        return true;
    }

    public void registerFailure(String ip) {
        attempts.compute(ip, (key, info) -> {
            int count = (info == null ? 0 : info.failures()) + 1;
            LocalDateTime block = count >= MAX_FAILURES
                    ? LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES)
                    : (info == null ? null : info.blockedUntil());
            return new AttemptInfo(count, block);
        });
    }

    public void registerSuccess(String ip) {
        attempts.remove(ip);
    }

    public int remainingAttempts(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null) return MAX_FAILURES;
        return Math.max(0, MAX_FAILURES - info.failures());
    }
}
