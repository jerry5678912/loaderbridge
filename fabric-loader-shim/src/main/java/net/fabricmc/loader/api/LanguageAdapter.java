package net.fabricmc.loader.api;

import dev.loaderbridge.fabric.runtime.BridgeDefaultLanguageAdapter;

public interface LanguageAdapter {
    static LanguageAdapter getDefault() { return BridgeDefaultLanguageAdapter.INSTANCE; }

    <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException;
}
