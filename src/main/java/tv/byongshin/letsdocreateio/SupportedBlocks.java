package tv.byongshin.letsdocreateio;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;

/** Audited against the versions listed in README; no Let's Do class linkage. */
public final class SupportedBlocks {
    private SupportedBlocks() {}
    private static List<Integer> range(int start, int end) { return IntStream.rangeClosed(start, end).boxed().toList(); }
    private static final SlotLayout POT = new SlotLayout(8, range(0, 6), List.of(), List.of(7));
    private static TagKey<Item> tag(String id) { return TagKey.create(Registries.ITEM, new ResourceLocation(id)); }
    private static boolean isFuel(ItemStack stack) { return ForgeHooks.getBurnTime(stack, null) > 0; }
    // Natively ingredients enter only from the top, where a pot usually sits; sides take fuel, and now non-fuel ingredients.
    private static final SlotLayout STOVE = new SlotLayout(5, range(1, 3), List.of(4), List.of(0), List.of(
        new SlotLayout.Route(SupportedBlocks::isFuel, List.of(4)),
        new SlotLayout.Route(stack -> true, range(1, 3))));
    // Created on first use: registry keys need the game bootstrap, which plain unit tests do not run.
    private static final class Water {
        static final TagKey<Item> SMALL = tag("meadow:small_water_fill");
        static final TagKey<Item> LARGE = tag("meadow:large_water_fill");
    }
    private static final SlotLayout PAIR = new SlotLayout(3, List.of(1, 2), List.of(), List.of(0));
    /**
     * Internal water level NBT key and maximum, for blocks filled from water items; exposed as a fill-only tank.
     * {@code fillSlot} holds water-fill items ({@code <namespace>:small_water_fill} adds 25, {@code large_water_fill} 50);
     * natively the block consumes one per tick even at full level, so automation may only fill it up to the level's room.
     */
    public record WaterLevel(String nbtKey, int maxUnits, int fillSlot, String tagNamespace) {
        public int unitsOf(ItemStack stack) {
            if (stack.is(tag(tagNamespace + ":small_water_fill"))) return 25;
            if (stack.is(tag(tagNamespace + ":large_water_fill"))) return 50;
            return 0;
        }
    }
    public static final Map<String, WaterLevel> WATER = Map.of(
        "meadow:cooking_cauldron", new WaterLevel("FluidLevel", 100, 7, "meadow"),
        "herbalbrews:tea_kettle", new WaterLevel("WaterLevel", 100, 6, "herbalbrews"));
    public static final Map<String, SlotLayout> TABLE = Map.ofEntries(
        Map.entry("bakery:small_cooking_pot", POT),
        Map.entry("beachparty:tiki_bar", PAIR),
        Map.entry("beachparty:mini_fridge", PAIR),
        Map.entry("brewery:brewingstation", new SlotLayout(3, range(0, 2), List.of(), List.of())),
        Map.entry("candlelight:cooking_pot", POT),
        Map.entry("candlelight:cooking_pan", POT),
        Map.entry("candlelight:stove_block", STOVE),
        Map.entry("farm_and_charm:roaster", POT),
        Map.entry("farm_and_charm:cooking_pot", POT),
        Map.entry("farm_and_charm:stove_block", STOVE),
        Map.entry("farm_and_charm:silo", new SlotLayout(162, range(0, 80), List.of(), range(81, 161))),
        Map.entry("herbalbrews:tea_kettle", new SlotLayout(8, range(1, 6), List.of(7), List.of(0))),
        Map.entry("herbalbrews:cauldron", new SlotLayout(5, List.of(0, 1, 2, 4), List.of(), List.of(3))),
        Map.entry("meadow:cheese_form", PAIR),
        // Natively the water slot 7 fills only from below, where the heat source must be; sides route water fills there.
        Map.entry("meadow:cooking_cauldron", new SlotLayout(8, range(1, 7), List.of(), List.of(0), List.of(
            new SlotLayout.Route(stack -> stack.is(Water.SMALL) || stack.is(Water.LARGE), List.of(7)),
            new SlotLayout.Route(stack -> true, range(1, 6))))),
        // Slot 1 is the first recipe's output, consumed internally by the second recipe.
        Map.entry("vinery:apple_press", new SlotLayout(4, List.of(0, 2), List.of(), List.of(1, 3))),
        // Natively juice enters only from the top and ingredients only from the sides; sides also take juice.
        Map.entry("vinery:fermentation_barrel", new SlotLayout(6, range(0, 4), List.of(), List.of(5), List.of(), true))
    );
}
