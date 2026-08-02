package net.fabricmc.loader.api;

@SuppressWarnings({"deprecation", "serial"})
public class VersionParsingException extends net.fabricmc.loader.util.version.VersionParsingException {
    public VersionParsingException() {}
    public VersionParsingException(Throwable cause) { super(cause); }
    public VersionParsingException(String message) { super(message); }
    public VersionParsingException(String message, Throwable cause) { super(message, cause); }
}
