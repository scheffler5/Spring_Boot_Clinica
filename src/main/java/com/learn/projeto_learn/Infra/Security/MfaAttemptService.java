package com.learn.projeto_learn.Infra.Security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MfaAttemptService {

    public static final int MAX_ATTEMPTS = 3;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_RESENDS = 3;

    private record MfaState(int attempts, int resends, LocalDateTime lastResendAt) {}

    private final ConcurrentHashMap<String, MfaState> states = new ConcurrentHashMap<>();



    public int registerFailure(String email) {
        MfaState s = states.compute(email, (k, old) -> {
            int attempts = (old == null ? 0 : old.attempts()) + 1;
            int resends  = (old == null ? 0 : old.resends());
            LocalDateTime last = (old == null ? null : old.lastResendAt());
            return new MfaState(attempts, resends, last);
        });
        return Math.max(0, MAX_ATTEMPTS - s.attempts());
    }

    public boolean isExhausted(String email) {
        MfaState s = states.get(email);
        return s != null && s.attempts() >= MAX_ATTEMPTS;
    }

    public int remainingAttempts(String email) {
        MfaState s = states.get(email);
        if (s == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - s.attempts());
    }

    public void registerSuccess(String email) {
        states.remove(email);
    }



    public boolean canResend(String email) {
        MfaState s = states.get(email);
        if (s == null) return true;
        if (s.resends() >= MAX_RESENDS) return false;
        if (s.lastResendAt() != null &&
                LocalDateTime.now().isBefore(s.lastResendAt().plusSeconds(RESEND_COOLDOWN_SECONDS))) {
            return false;
        }
        return true;
    }

    public void registerResend(String email) {
        states.compute(email, (k, old) -> {
            int resends = (old == null ? 0 : old.resends()) + 1;
            return new MfaState(0, resends, LocalDateTime.now());
        });
    }

    public int resendCooldownSecondsRemaining(String email) {
        MfaState s = states.get(email);
        if (s == null || s.lastResendAt() == null) return 0;
        long elapsed = java.time.Duration.between(s.lastResendAt(), LocalDateTime.now()).getSeconds();
        return (int) Math.max(0, RESEND_COOLDOWN_SECONDS - elapsed);
    }

    public int remainingResends(String email) {
        MfaState s = states.get(email);
        if (s == null) return MAX_RESENDS;
        return Math.max(0, MAX_RESENDS - s.resends());
    }
}
