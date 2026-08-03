package net.minecraft.client;

public final class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();

    private Minecraft() {}

    public static Minecraft getInstance() {
        return INSTANCE;
    }
}
