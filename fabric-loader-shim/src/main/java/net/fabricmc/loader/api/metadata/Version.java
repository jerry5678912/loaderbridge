package net.fabricmc.loader.api.metadata;

public interface Version extends Comparable<Version> {
    String getFriendlyString();
}
