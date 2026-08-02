package dev.loaderbridge.scenario.yaml;

import java.io.IOException;

public final class ScenarioFormatException extends IOException {
    private static final long serialVersionUID = 1L;

    public ScenarioFormatException(String message) {
        super(message);
    }

    public ScenarioFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
