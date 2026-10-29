package tv.byongshin.letsdocreateio;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SlotLayoutTest {
    @Test void rejectsOverlappingAndOutOfRangeSlots() {
        assertThrows(IllegalArgumentException.class, () -> new SlotLayout(2, List.of(0), List.of(), List.of(0)));
        assertThrows(IllegalArgumentException.class, () -> new SlotLayout(2, List.of(2), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SlotLayout(2, List.of(-1), List.of(), List.of()));
    }
    @Test void layoutsNeverPermitBothDirections() {
        for (var layout : SupportedBlocks.TABLE.values()) {
            for (int slot = -1; slot <= layout.size(); slot++) {
                assertFalse(layout.canInsert(slot) && layout.canExtract(slot));
            }
            assertFalse(layout.canInsert(-1));
            assertFalse(layout.canExtract(layout.size()));
        }
    }
    @Test void insertionRespectsBothLimitsAndFullContainers() {
        assertEquals(4, SlotLayout.insertionCount(20, 12, 64, 16));
        assertEquals(2, SlotLayout.insertionCount(20, 6, 8, 64));
        assertEquals(0, SlotLayout.insertionCount(20, 64, 64, 64));
        assertEquals(0, SlotLayout.insertionCount(20, 65, 64, 64));
        assertEquals(1, SlotLayout.insertionCount(1, 0, 64, 64));
    }
    @Test void immutableLayoutPreservesRoles() {
        var inputs = new java.util.ArrayList<>(List.of(0));
        var layout = new SlotLayout(3, inputs, List.of(1), List.of(2));
        inputs.clear();
        assertTrue(layout.canInsert(0));
        assertTrue(layout.canInsert(1));
        assertTrue(layout.canExtract(2));
        assertThrows(UnsupportedOperationException.class, () -> layout.outputs().clear());
    }
    @Test void sideRoutesOnlyReachInsertableSlots() {
        assertThrows(IllegalArgumentException.class, () -> new SlotLayout(3, List.of(0), List.of(), List.of(2),
            List.of(new SlotLayout.Route(stack -> true, List.of(2)))));
        var layout = new SlotLayout(3, List.of(0), List.of(1), List.of(2), List.of(new SlotLayout.Route(stack -> false, List.of(1))));
        assertEquals(List.of(), layout.sideSlots(null));
        assertNull(new SlotLayout(3, List.of(0), List.of(), List.of(2)).sideSlots(null));
    }
}
