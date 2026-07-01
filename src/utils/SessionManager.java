package utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    public static final String SESSION_COOKIE = "JSESSIONID";
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public record SessionResult(Session session, boolean created) {
    }

    public static class Session {
        public final String id;
        public final Map<String, Object> attributes = new ConcurrentHashMap<>();
        public long lastAccess = System.currentTimeMillis();

        public Session(String id) {
            this.id = id;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccess > SESSION_TIMEOUT_MS;
        }

        public void touch() {
            lastAccess = System.currentTimeMillis();
        }
    }

    public static SessionResult getOrCreate(String sessionId) {
        if (sessionId != null && sessions.containsKey(sessionId)) {
            Session session = sessions.get(sessionId);
            if (!session.isExpired()) {
                session.touch();
                return new SessionResult(session, false);
            } else
                sessions.remove(sessionId);
        }
        String newId = generateId();
        Session newSession = new Session(newId);
        sessions.put(newId, newSession);
        return new SessionResult(newSession, true);
    }

    public static Session get(String sessionId) {
        if (sessionId == null)
            return null;
        Session sd = sessions.get(sessionId);
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
        Iterator<Map.Entry<String, Session>> it = sessions.entrySet().iterator();
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
