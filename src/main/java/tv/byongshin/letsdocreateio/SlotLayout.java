package tv.byongshin.letsdocreateio;

import java.util.List;

/** Original Container indices; unspecified slots stay inaccessible. */
public record SlotLayout(int size, List<Integer> inputs, List<Integer> fuel, List<Integer> outputs) {
    public SlotLayout {
        inputs = List.copyOf(inputs);
        fuel = List.copyOf(fuel);
        outputs = List.copyOf(outputs);
        var seen = new java.util.HashSet<Integer>();
        for (var group : List.of(inputs, fuel, outputs)) {
            for (int slot : group) {
                if (slot < 0 || slot >= size || !seen.add(slot)) {
                    throw new IllegalArgumentException("Invalid or overlapping slot: " + slot);
                }
            }
        }
    }
    public boolean canInsert(int slot) { return inputs.contains(slot) || fuel.contains(slot); }
    public boolean canExtract(int slot) { return outputs.contains(slot); }

    public static int insertionCount(int requested, int existing, int containerLimit, int itemLimit) {
        return Math.max(0, Math.min(requested, Math.min(containerLimit, itemLimit) - existing));
    }
}
