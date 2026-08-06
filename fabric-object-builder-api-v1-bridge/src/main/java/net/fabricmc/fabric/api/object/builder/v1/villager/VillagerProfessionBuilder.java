package net.fabricmc.fabric.api.object.builder.v1.villager;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Deprecated Fabric builder retained for binary compatibility. */
@Deprecated
public final class VillagerProfessionBuilder {
    private final ImmutableSet.Builder<Item> gatherableItemsBuilder = ImmutableSet.builder();
    private final ImmutableSet.Builder<Block> secondaryJobSiteBlockBuilder =
            ImmutableSet.builder();
    private ResourceLocation identifier;
    private Predicate<Holder<PoiType>> pointOfInterestType;
    private Predicate<Holder<PoiType>> acquirableJobSite;
    private SoundEvent workSoundEvent;

    private VillagerProfessionBuilder() {}

    public static VillagerProfessionBuilder create() {
        return new VillagerProfessionBuilder();
    }

    public VillagerProfessionBuilder id(ResourceLocation id) {
        this.identifier = id;
        return this;
    }

    public VillagerProfessionBuilder workstation(ResourceKey<PoiType> key) {
        jobSite(entry -> entry.is(key));
        return workstation(entry -> entry.is(key));
    }

    public VillagerProfessionBuilder workstation(Predicate<Holder<PoiType>> predicate) {
        this.pointOfInterestType = predicate;
        return this;
    }

    public VillagerProfessionBuilder jobSite(Predicate<Holder<PoiType>> predicate) {
        this.acquirableJobSite = predicate;
        return this;
    }

    public VillagerProfessionBuilder harvestableItems(Item... items) {
        this.gatherableItemsBuilder.add(items);
        return this;
    }

    public VillagerProfessionBuilder harvestableItems(Iterable<Item> items) {
        this.gatherableItemsBuilder.addAll(items);
        return this;
    }

    public VillagerProfessionBuilder secondaryJobSites(Block... blocks) {
        this.secondaryJobSiteBlockBuilder.add(blocks);
        return this;
    }

    public VillagerProfessionBuilder secondaryJobSites(Iterable<Block> blocks) {
        this.secondaryJobSiteBlockBuilder.addAll(blocks);
        return this;
    }

    public VillagerProfessionBuilder workSound(SoundEvent workSoundEvent) {
        this.workSoundEvent = workSoundEvent;
        return this;
    }

    public VillagerProfession build() {
        checkState(identifier != null,
                "An Identifier is required to build a new VillagerProfession.");
        checkState(pointOfInterestType != null,
                "A PointOfInterestType is required to build a new VillagerProfession.");
        checkState(acquirableJobSite != null,
                "A PointOfInterestType is required for the acquirableJobSite to build a new "
                        + "VillagerProfession.");
        return new VillagerProfession(identifier.toString(), pointOfInterestType,
                acquirableJobSite, gatherableItemsBuilder.build(),
                secondaryJobSiteBlockBuilder.build(), workSoundEvent);
    }
}
