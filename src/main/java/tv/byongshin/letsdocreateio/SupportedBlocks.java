package tv.byongshin.letsdocreateio;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Audited against the versions listed in README; no Let's Do class linkage. */
public final class SupportedBlocks {
    private SupportedBlocks() {}
    private static List<Integer> range(int start, int end) { return IntStream.rangeClosed(start, end).boxed().toList(); }
    private static final SlotLayout POT = new SlotLayout(8, range(0, 6), List.of(), List.of(7));
    private static final SlotLayout STOVE = new SlotLayout(5, range(1, 3), List.of(4), List.of(0));
    private static final SlotLayout PAIR = new SlotLayout(3, List.of(1, 2), List.of(), List.of(0));
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
        Map.entry("meadow:cooking_cauldron", new SlotLayout(8, range(1, 7), List.of(), List.of(0))),
        // Slot 1 is the first recipe's output, consumed internally by the second recipe.
        Map.entry("vinery:apple_press", new SlotLayout(4, List.of(0, 2), List.of(), List.of(1, 3))),
        Map.entry("vinery:fermentation_barrel", new SlotLayout(6, range(0, 4), List.of(), List.of(5)))
    );
}
