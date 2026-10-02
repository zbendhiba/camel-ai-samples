package org.acme.ward;

import java.net.ServerSocket;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Gives the tests their own free MLLP port, the way Quarkus manages the HTTP test port:
 * the configuration is overridden for the test run, and everything that reads
 * {@code ward.feed.port} follows. Tests then run happily next to a dev-mode instance
 * holding the default port.
 */
public class FreeFeedPortTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return Map.of("ward.feed.port", String.valueOf(socket.getLocalPort()));
        } catch (Exception e) {
            throw new RuntimeException("Could not pick a free MLLP port for the tests", e);
        }
    }

    @Override
    public void stop() {
    }
}
