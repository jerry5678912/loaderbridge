package net.fabricmc.loader.api;

/** Exception raised while resolving a Fabric entrypoint. */
@SuppressWarnings("serial")
public class EntrypointException extends RuntimeException {
    private final String key;

    /** @deprecated Fabric Loader compatibility constructor. */
    @Deprecated
    public EntrypointException(String key, Throwable cause) {
        super("Exception while loading entries for entrypoint '" + key + "'!", cause);
        this.key = key;
    }

    /** @deprecated Fabric Loader compatibility constructor. */
    @Deprecated
    public EntrypointException(String key, String causingMod, Throwable cause) {
        super("Exception while loading entries for entrypoint '" + key + "' provided by '"
                + causingMod + "'", cause);
        this.key = key;
    }

    /** @deprecated Fabric Loader compatibility constructor. */
    @Deprecated
    public EntrypointException(String message) {
        super(message);
        this.key = "";
    }

    /** @deprecated Fabric Loader compatibility constructor. */
    @Deprecated
    public EntrypointException(Throwable cause) {
        super(cause);
        this.key = "";
    }

    public String getKey() {
        return key;
    }
}
