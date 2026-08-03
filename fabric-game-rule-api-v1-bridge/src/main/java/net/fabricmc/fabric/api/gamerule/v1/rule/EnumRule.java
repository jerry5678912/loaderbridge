package net.fabricmc.fabric.api.gamerule.v1.rule;

import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public final class EnumRule<E extends Enum<E>> extends GameRules.Value<EnumRule<E>> {
    private final Class<E> classType;
    private final List<E> supportedValues;
    private E value;

    @Deprecated public EnumRule(GameRules.Type<EnumRule<E>> type, E value, E[] supportedValues) {
        this(type, value, Arrays.asList(supportedValues));
    }
    @Deprecated public EnumRule(GameRules.Type<EnumRule<E>> type, E value,
            Collection<E> supportedValues) {
        super(type);
        this.value = Objects.requireNonNull(value);
        this.classType = value.getDeclaringClass();
        this.supportedValues = new ArrayList<>(supportedValues);
        if (!supports(value)) throw new IllegalArgumentException("Unsupported default enum value");
    }
    @Override protected void updateFromArgument(CommandContext<CommandSourceStack> context, String name) {
        deserialize(context.getArgument(name, String.class));
    }
    @Override protected void deserialize(String serialized) {
        try { set(Enum.valueOf(classType, serialized), null); } catch (IllegalArgumentException ignored) { }
    }
    @Override public String serialize() { return value.name(); }
    @Override public int getCommandResult() { return value.ordinal(); }
    @Override protected EnumRule<E> getSelf() { return this; }
    @Override protected EnumRule<E> copy() { return new EnumRule<>(type, value, supportedValues); }
    @Override public void setFrom(EnumRule<E> rule, MinecraftServer server) { set(rule.value, server); }
    public Class<E> getEnumClass() { return classType; }
    public E get() { return value; }
    public void cycle() { set(supportedValues.get((supportedValues.indexOf(value) + 1) % supportedValues.size()), null); }
    public boolean supports(E next) { return supportedValues.contains(next); }
    public void set(E next, MinecraftServer server) {
        Objects.requireNonNull(next);
        if (!supports(next)) throw new IllegalArgumentException("Unsupported enum value: " + next);
        value = next;
        onChanged(server);
    }
}
