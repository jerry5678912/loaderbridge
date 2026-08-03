package dev.loaderbridge.fabric.api.object.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;

public final class BridgeTradeOffers {
    private static final Map<VillagerProfession, Map<Integer,
            List<TradeOfferHelper.VillagerOffersAdder>>> VILLAGER = new LinkedHashMap<>();
    private static final List<Consumer<List<VillagerTrades.ItemListing>>> COMMON = new ArrayList<>();
    private static final List<Consumer<List<VillagerTrades.ItemListing>>> RARE = new ArrayList<>();

    private BridgeTradeOffers() {}

    public static synchronized void registerVillager(VillagerProfession profession, int level,
            TradeOfferHelper.VillagerOffersAdder adder) {
        if (level < 1 || level > 5) throw new IllegalArgumentException("Villager level must be 1-5");
        VILLAGER.computeIfAbsent(profession, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(level, ignored -> new ArrayList<>()).add(adder);
    }

    public static synchronized void registerWandering(int rarity,
            Consumer<List<VillagerTrades.ItemListing>> consumer) {
        if (rarity == 1) COMMON.add(consumer);
        else if (rarity == 2) RARE.add(consumer);
        else throw new IllegalArgumentException("Wandering trader rarity must be 1 or 2");
    }

    public static void apply(VillagerTradesEvent event) {
        Map<Integer, List<TradeOfferHelper.VillagerOffersAdder>> levels;
        synchronized (BridgeTradeOffers.class) { levels = VILLAGER.get(event.getType()); }
        if (levels == null) return;
        levels.forEach((level, adders) -> {
            List<VillagerTrades.ItemListing> offers = event.getTrades().get(level.intValue());
            if (offers != null) adders.forEach(adder -> adder.onRegister(offers, false));
        });
    }

    public static void apply(WandererTradesEvent event) {
        synchronized (BridgeTradeOffers.class) {
            COMMON.forEach(callback -> callback.accept(event.getGenericTrades()));
            RARE.forEach(callback -> callback.accept(event.getRareTrades()));
        }
    }

    public static TradeOfferHelper.WanderingTraderOffersBuilder rebalancedBuilder() {
        return new TradeOfferHelper.WanderingTraderOffersBuilder() {
            @Override public TradeOfferHelper.WanderingTraderOffersBuilder pool(
                    ResourceLocation pool, int count, VillagerTrades.ItemListing... offers) {
                List<VillagerTrades.ItemListing> selected = Arrays.stream(offers)
                        .limit(Math.max(0, count)).toList();
                return addOffersToPool(pool,
                        selected.toArray(VillagerTrades.ItemListing[]::new));
            }

            @Override public TradeOfferHelper.WanderingTraderOffersBuilder addOffersToPool(
                    ResourceLocation pool, VillagerTrades.ItemListing... offers) {
                registerWandering(pool.equals(TradeOfferHelper.WanderingTraderOffersBuilder
                        .SELL_SPECIAL_ITEMS_POOL) ? 2 : 1, list -> list.addAll(Arrays.asList(offers)));
                return this;
            }
        };
    }
}
