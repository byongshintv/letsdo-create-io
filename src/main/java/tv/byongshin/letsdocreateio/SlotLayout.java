package tv.byongshin.letsdocreateio;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/**
 * Original Container indices; unspecified slots stay inaccessible.
 * {@code sideRoutes}, when present, replace the block's own horizontal-face slots: an item inserted from a side goes
 * only to the slots of the first route that accepts it. Used where the block's native face for an input is the one a
 * required neighbour covers (a pot on a stove's top, heat under a cauldron).
 * {@code sidesUseTop} lets a horizontal face also fill what the block's own top face takes, so one side-mounted
 * packager can unpack a mixed order (fermentation barrel: juice natively only from the top, ingredients only from sides).
 */
public record SlotLayout(int size, List<Integer> inputs, List<Integer> fuel, List<Integer> outputs, List<Route> sideRoutes,
                         boolean sidesUseTop) {
    public record Route(Predicate<ItemStack> accepts, List<Integer> slots) {
        public Route { slots = List.copyOf(slots); }
    }
    public SlotLayout(int size, List<Integer> inputs, List<Integer> fuel, List<Integer> outputs) {
        this(size, inputs, fuel, outputs, List.of(), false);
    }
    public SlotLayout(int size, List<Integer> inputs, List<Integer> fuel, List<Integer> outputs, List<Route> sideRoutes) {
        this(size, inputs, fuel, outputs, sideRoutes, false);
    }
    public SlotLayout {
        inputs = List.copyOf(inputs);
        fuel = List.copyOf(fuel);
        outputs = List.copyOf(outputs);
        sideRoutes = List.copyOf(sideRoutes);
        var seen = new java.util.HashSet<Integer>();
        for (var group : List.of(inputs, fuel, outputs)) {
            for (int slot : group) {
                if (slot < 0 || slot >= size || !seen.add(slot)) {
                    throw new IllegalArgumentException("Invalid or overlapping slot: " + slot);
                }
            }
        }
        for (var route : sideRoutes) {
            for (int slot : route.slots()) {
                if (!inputs.contains(slot) && !fuel.contains(slot)) {
                    throw new IllegalArgumentException("Side route to non-insertable slot: " + slot);
                }
            }
        }
    }
    public boolean canInsert(int slot) { return inputs.contains(slot) || fuel.contains(slot); }
    public boolean canExtract(int slot) { return outputs.contains(slot); }
    /** Slots a horizontal face may fill with this item, or null when the block's own face rules apply. */
    public List<Integer> sideSlots(ItemStack stack) {
        if (sideRoutes.isEmpty()) return null;
        for (var route : sideRoutes) {
            if (route.accepts().test(stack)) return route.slots();
        }
        return List.of();
    }

    public static int insertionCount(int requested, int existing, int containerLimit, int itemLimit) {
        return Math.max(0, Math.min(requested, Math.min(containerLimit, itemLimit) - existing));
    }
}
