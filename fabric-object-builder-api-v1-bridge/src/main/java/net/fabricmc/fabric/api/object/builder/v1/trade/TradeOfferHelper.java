package net.fabricmc.fabric.api.object.builder.v1.trade;

import dev.loaderbridge.fabric.api.object.builder.BridgeTradeOffers;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

/** Fabric trade registration mapped to Forge's trade-construction events. */
public final class TradeOfferHelper {
    private TradeOfferHelper() {}

    public static void registerVillagerOffers(VillagerProfession profession, int level,
            Consumer<List<VillagerTrades.ItemListing>> consumer) {
        registerVillagerOffers(profession, level, (offers, rebalanced) -> consumer.accept(offers));
    }

    public static void registerVillagerOffers(VillagerProfession profession, int level,
            VillagerOffersAdder adder) {
        BridgeTradeOffers.registerVillager(profession, level, adder);
    }

    public static void registerWanderingTraderOffers(int rarity,
            Consumer<List<VillagerTrades.ItemListing>> consumer) {
        BridgeTradeOffers.registerWandering(rarity, consumer);
    }

    public static synchronized void registerRebalancedWanderingTraderOffers(
            Consumer<WanderingTraderOffersBuilder> consumer) {
        consumer.accept(BridgeTradeOffers.rebalancedBuilder());
    }

    /** Fabric retains this deprecated hook as a warning-only compatibility method. */
    public static void refreshOffers() {}

    @FunctionalInterface
    public interface VillagerOffersAdder {
        void onRegister(List<VillagerTrades.ItemListing> offers, boolean rebalanced);
    }

    public interface WanderingTraderOffersBuilder {
        ResourceLocation BUY_ITEMS_POOL = ResourceLocation.withDefaultNamespace("buy_items");
        ResourceLocation SELL_SPECIAL_ITEMS_POOL =
                ResourceLocation.withDefaultNamespace("sell_special_items");
        ResourceLocation SELL_COMMON_ITEMS_POOL =
                ResourceLocation.withDefaultNamespace("sell_common_items");

        WanderingTraderOffersBuilder pool(ResourceLocation pool, int count,
                VillagerTrades.ItemListing... offers);

        default WanderingTraderOffersBuilder pool(ResourceLocation pool, int count,
                Collection<? extends VillagerTrades.ItemListing> offers) {
            return pool(pool, count, offers.toArray(VillagerTrades.ItemListing[]::new));
        }

        default WanderingTraderOffersBuilder addAll(ResourceLocation pool,
                Collection<? extends VillagerTrades.ItemListing> offers) {
            return pool(pool, offers.size(), offers);
        }

        default WanderingTraderOffersBuilder addAll(ResourceLocation pool,
                VillagerTrades.ItemListing... offers) {
            return pool(pool, offers.length, offers);
        }

        WanderingTraderOffersBuilder addOffersToPool(ResourceLocation pool,
                VillagerTrades.ItemListing... offers);

        default WanderingTraderOffersBuilder addOffersToPool(ResourceLocation pool,
                Collection<VillagerTrades.ItemListing> offers) {
            return addOffersToPool(pool, offers.toArray(VillagerTrades.ItemListing[]::new));
        }
    }
}
