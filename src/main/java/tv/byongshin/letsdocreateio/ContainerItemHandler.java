package tv.byongshin.letsdocreateio;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class ContainerItemHandler implements IItemHandler {
    private final Container container;
    private final SlotLayout layout;
    private final BooleanSupplier enabled;
    private final IntPredicate active;
    private final Runnable changed;

    public ContainerItemHandler(Container container, SlotLayout layout, BooleanSupplier enabled,
                                IntPredicate active, Runnable changed) {
        this.container = container;
        this.layout = layout;
        this.enabled = enabled;
        this.active = active;
        this.changed = changed;
    }
    private boolean accessible(int slot) {
        return slot >= 0 && slot < getSlots() && container.getContainerSize() == layout.size()
            && enabled.getAsBoolean() && active.test(slot);
    }
    @Override public int getSlots() { return layout.size(); }
    @Override public ItemStack getStackInSlot(int slot) {
        return accessible(slot) ? container.getItem(slot).copy() : ItemStack.EMPTY;
    }
    @Override public int getSlotLimit(int slot) { return accessible(slot) ? container.getMaxStackSize() : 0; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return accessible(slot) && layout.canInsert(slot) && !stack.isEmpty() && container.canPlaceItem(slot, stack);
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) return stack;
        ItemStack current = container.getItem(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, stack)) return stack;
        int count = SlotLayout.insertionCount(stack.getCount(), current.getCount(), getSlotLimit(slot), stack.getMaxStackSize());
        if (count == 0) return stack;
        if (!simulate) {
            ItemStack updated = stack.copyWithCount(current.getCount() + count);
            container.setItem(slot, updated);
            changed.run();
        }
        return stack.getCount() == count ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - count);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!accessible(slot) || !layout.canExtract(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = container.getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        int count = Math.min(amount, Math.min(current.getCount(), current.getMaxStackSize()));
        ItemStack result = current.copyWithCount(count);
        if (!simulate) {
            container.setItem(slot, current.getCount() == count ? ItemStack.EMPTY : current.copyWithCount(current.getCount() - count));
            changed.run();
        }
        return result;
    }
}
