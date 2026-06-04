package utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    public static final String SESSION_COOKIE = "JSESSIONID";
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public static class SessionData {
        public final String id;
        public final Map<String, Object> attributes = new ConcurrentHashMap<>();
        public long lastAccess = System.currentTimeMillis();

        public SessionData(String id) {
            this.id = id;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccess > SESSION_TIMEOUT_MS;
        }

        public void touch() {
            lastAccess = System.currentTimeMillis();
        }
    }

    public static String[] getOrCreate(String sessionId) {
        if (sessionId != null && sessions.containsKey(sessionId)) {
            SessionData sd = sessions.get(sessionId);
            if (!sd.isExpired()) {
                sd.touch();
                return new String[] { sessionId, "false" };
            } else
                sessions.remove(sessionId);
        }
        String newId = generateId();
        sessions.put(newId, new SessionData(newId));
        return new String[] { newId, "true" };
    }

    public static SessionData get(String sessionId) {
        if (sessionId == null)
            return null;
        SessionData sd = sessions.get(sessionId);
        if (sd != null && sd.isExpired()) {
            sessions.remove(sessionId);
            return null;
        }
        if (sd != null)
            sd.touch();
        return sd;
    }

    public static void invalidate(String sessionId) {
        sessions.remove(sessionId);
    }

    public static int purgeExpired() {
        int count = 0;
        Iterator<Map.Entry<String, SessionData>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    public static int getActiveCount() {
        return sessions.size();
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
