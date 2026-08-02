package net.fabricmc.loader.util.version;

@Deprecated
@SuppressWarnings("serial")
public class VersionParsingException extends Exception {
    public VersionParsingException() {}
    public VersionParsingException(Throwable cause) { super(cause); }
    public VersionParsingException(String message) { super(message); }
    public VersionParsingException(String message, Throwable cause) { super(message, cause); }
}
