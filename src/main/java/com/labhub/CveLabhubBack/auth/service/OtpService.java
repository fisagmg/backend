package com.labhub.CveLabhubBack.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private static class Entry { String code; Instant expireAt; }
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Random rnd = new Random();

    public String generate(String email) {
        String code = String.format("%06d", rnd.nextInt(1_000_000));
        Entry e = new Entry();
        e.code = code;
        e.expireAt = Instant.now().plusSeconds(300); // 5분
        store.put(email.toLowerCase(), e);
        return code;
    }

    public boolean verify(String email, String code) {
        Entry e = store.get(email.toLowerCase());
        if (e == null) return false;
        if (Instant.now().isAfter(e.expireAt)) return false;
        return e.code.equals(code);
    }

    public void consume(String email) { store.remove(email.toLowerCase()); }
}
