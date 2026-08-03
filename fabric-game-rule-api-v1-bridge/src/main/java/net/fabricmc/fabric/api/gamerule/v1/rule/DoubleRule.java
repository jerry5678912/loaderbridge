package net.fabricmc.fabric.api.gamerule.v1.rule;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public final class DoubleRule extends GameRules.Value<DoubleRule> implements ValidateableRule {
    private final double minimumValue;
    private final double maximumValue;
    private double value;

    @Deprecated
    public DoubleRule(GameRules.Type<DoubleRule> type, double value, double minimumValue,
            double maximumValue) {
        super(type);
        if (!Double.isFinite(value) || !Double.isFinite(minimumValue) || !Double.isFinite(maximumValue)) {
            throw new IllegalArgumentException("Double rule values must be finite");
        }
        this.value = value;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }
    @Override protected void updateFromArgument(CommandContext<CommandSourceStack> context, String name) {
        value = context.getArgument(name, Double.class);
    }
    @Override protected void deserialize(String serialized) { validate(serialized); }
    @Override public String serialize() { return Double.toString(value); }
    @Override public int getCommandResult() { return Double.compare(value, 0.0D); }
    @Override protected DoubleRule getSelf() { return this; }
    @Override protected DoubleRule copy() { return new DoubleRule(type, value, minimumValue, maximumValue); }
    @Override public void setFrom(DoubleRule rule, MinecraftServer server) {
        set(rule.value, server);
    }
    public double get() { return value; }
    public void set(double next, MinecraftServer server) {
        if (!inBounds(next)) throw new IllegalArgumentException("Double rule value is out of bounds");
        value = next;
        onChanged(server);
    }
    @Override public boolean validate(String serialized) {
        try {
            double next = Double.parseDouble(serialized);
            if (!inBounds(next)) return false;
            value = next;
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
    private boolean inBounds(double next) {
        return Double.isFinite(next) && next >= minimumValue && next <= maximumValue;
    }
}
