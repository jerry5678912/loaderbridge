package net.fabricmc.loader.api;

@SuppressWarnings("serial")
public class LanguageAdapterException extends Exception {
    public LanguageAdapterException(String message) { super(message); }
    public LanguageAdapterException(Throwable cause) { super(cause); }
    public LanguageAdapterException(String message, Throwable cause) { super(message, cause); }
}
