package tv.byongshin.letsdocreateio;

import java.util.function.BooleanSupplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Fill-only water tank over a block's internal 0..max water level, so any Forge fluid pipe (Create, Mekanism,
 * Pipez, ...) can water it. The level lives only in the block's NBT field; it is read and written through the
 * public save/load round trip instead of linking to Let's Do classes or reflecting on private fields.
 * One level unit is {@link #MB_PER_UNIT} mB: a water bucket adds 25 units natively, so 1000 mB = 25 units.
 * Sub-unit remainders are buffered in memory and lost on unload (under one unit).
 */
public final class WaterLevelHandler implements IFluidHandler {
    public static final int MB_PER_UNIT = 40;
    private final BlockEntity entity;
    private final String key;
    private final int maxUnits;
    private final BooleanSupplier enabled;
    private final Runnable changed;
    private int bufferedMb;

    public WaterLevelHandler(BlockEntity entity, String key, int maxUnits, BooleanSupplier enabled, Runnable changed) {
        this.entity = entity;
        this.key = key;
        this.maxUnits = maxUnits;
        this.enabled = enabled;
        this.changed = changed;
    }
    private int units() {
        return Math.max(0, Math.min(maxUnits, entity.saveWithoutMetadata().getInt(key)));
    }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) {
        int mb = tank == 0 && enabled.getAsBoolean() ? units() * MB_PER_UNIT + bufferedMb : 0;
        return mb > 0 ? new FluidStack(Fluids.WATER, mb) : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int tank) { return tank == 0 ? maxUnits * MB_PER_UNIT : 0; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && stack.getFluid().isSame(Fluids.WATER) && !stack.hasTag();
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource) || !enabled.getAsBoolean()) return 0;
        int level = units();
        int accepted = Math.max(0, Math.min(resource.getAmount(), (maxUnits - level) * MB_PER_UNIT - bufferedMb));
        if (accepted == 0 || action.simulate()) return accepted;
        int total = bufferedMb + accepted;
        bufferedMb = total % MB_PER_UNIT;
        if (total / MB_PER_UNIT > 0) {
            var tag = entity.saveWithoutMetadata();
            tag.putInt(key, level + total / MB_PER_UNIT);
            entity.load(tag);
            changed.run();
        }
        return accepted;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
    @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
}
