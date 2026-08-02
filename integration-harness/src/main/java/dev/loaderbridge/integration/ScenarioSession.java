package dev.loaderbridge.integration;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface ScenarioSession extends AutoCloseable {
    void start(Duration timeout) throws Exception;

    boolean awaitLog(String marker, Duration timeout) throws Exception;

    void sendCommand(String command, Duration timeout) throws Exception;

    void reload(Duration timeout) throws Exception;

    boolean shutdown(String marker, Duration timeout) throws Exception;

    List<Path> artifacts();

    @Override
    default void close() {
    }
}
