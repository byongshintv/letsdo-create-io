package tv.byongshin.letsdocreateio;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * One handler per face. Insertion follows the block's own WorldlyContainer face rules (or the layout's side routes);
 * outputs can be taken from every face, because a pot's bottom sits on its heat source and a stove's top holds a pot.
 * The unsided (null) handler keeps every table slot open.
 */
public final class ContainerItemHandler implements IItemHandler {
    private final Supplier<Container> target;
    private final @Nullable Direction side;
    private final SlotLayout layout;
    private final BooleanSupplier enabled;
    private final IntPredicate active;
    private final Consumer<Container> changed;

    public ContainerItemHandler(Container container, SlotLayout layout, BooleanSupplier enabled,
                                IntPredicate active, Runnable changed) {
        this(() -> container, null, layout, enabled, active, c -> changed.run());
    }
    /** {@code target} is resolved on every call, so a multiblock segment can forward to its current controller. */
    public ContainerItemHandler(Supplier<Container> target, @Nullable Direction side, SlotLayout layout, BooleanSupplier enabled,
                                IntPredicate active, Consumer<Container> changed) {
        this.target = target;
        this.side = side;
        this.layout = layout;
        this.enabled = enabled;
        this.active = active;
        this.changed = changed;
    }
    private @Nullable Container container() {
        Container container = target.get();
        return container != null && container.getContainerSize() == layout.size() ? container : null;
    }
    private boolean accessible(int slot) {
        return slot >= 0 && slot < getSlots() && container() != null && enabled.getAsBoolean() && active.test(slot);
    }
    @Override public int getSlots() { return layout.size(); }
    @Override public ItemStack getStackInSlot(int slot) {
        return accessible(slot) ? container().getItem(slot).copy() : ItemStack.EMPTY;
    }
    @Override public int getSlotLimit(int slot) { return accessible(slot) ? container().getMaxStackSize() : 0; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return accessible(slot) && layout.canInsert(slot) && !stack.isEmpty() && faceAllows(slot, stack)
            && container().canPlaceItem(slot, stack);
    }
    private boolean faceAllows(int slot, ItemStack stack) {
        if (side == null) return true;
        if (side.getAxis().isHorizontal()) {
            var routed = layout.sideSlots(stack);
            if (routed != null) return routed.contains(slot);
        }
        if (!(container() instanceof WorldlyContainer worldly)) return true;
        for (int faceSlot : worldly.getSlotsForFace(side)) {
            if (faceSlot == slot) return worldly.canPlaceItemThroughFace(slot, stack, side);
        }
        return false;
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) return stack;
        Container container = container();
        ItemStack current = container.getItem(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, stack)) return stack;
        int count = SlotLayout.insertionCount(stack.getCount(), current.getCount(), getSlotLimit(slot), stack.getMaxStackSize());
        if (count == 0) return stack;
        if (!simulate) {
            ItemStack updated = stack.copyWithCount(current.getCount() + count);
            container.setItem(slot, updated);
            changed.accept(container);
        }
        return stack.getCount() == count ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - count);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!accessible(slot) || !layout.canExtract(slot) || amount <= 0) return ItemStack.EMPTY;
        Container container = container();
        ItemStack current = container.getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        int count = Math.min(amount, Math.min(current.getCount(), current.getMaxStackSize()));
        ItemStack result = current.copyWithCount(count);
        if (!simulate) {
            container.setItem(slot, current.getCount() == count ? ItemStack.EMPTY : current.copyWithCount(current.getCount() - count));
            changed.accept(container);
        }
        return result;
    }
}
