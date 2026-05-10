package dz.freelance.shared.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage OTP en mémoire pour le développement local.
 * En production, remplacer par Redis (StringRedisTemplate).
 */
@Service
public class OtpStore {

    private record OtpEntry(String value, LocalDateTime expiresAt) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public void set(String key, String value, int expirationMinutes) {
        store.put(key, new OtpEntry(value, LocalDateTime.now().plusMinutes(expirationMinutes)));
    }

    public String get(String key) {
        OtpEntry entry = store.get(key);
        if (entry == null) return null;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            return null;
        }
        return entry.value();
    }

    public void delete(String key) {
        store.remove(key);
    }

    public boolean exists(String key) {
        return get(key) != null;
    }
}
