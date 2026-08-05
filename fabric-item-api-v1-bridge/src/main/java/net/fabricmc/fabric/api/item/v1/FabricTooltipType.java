package net.fabricmc.fabric.api.item.v1;

public interface FabricTooltipType {
    default boolean shouldDisplayAllInformation() { return false; }
}
