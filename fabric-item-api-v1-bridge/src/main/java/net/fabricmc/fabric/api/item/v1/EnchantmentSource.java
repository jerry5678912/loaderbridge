package net.fabricmc.fabric.api.item.v1;

public enum EnchantmentSource {
    VANILLA(true), MOD(true), DATA_PACK(false);

    private final boolean builtin;
    EnchantmentSource(boolean builtin) { this.builtin = builtin; }
    public boolean isBuiltin() { return builtin; }
}
