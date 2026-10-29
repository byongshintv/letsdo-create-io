package tv.byongshin.letsdocreateio;

import java.lang.reflect.Method;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The only reflective adapter. A multiblock silo keeps its whole inventory in one controller segment; every other
 * segment reports capacity 0. Automation on any segment is forwarded to the controller (owner report 10-29: funnels
 * on a multiblock silo did nothing), using the silo's public getControllerBE and getCapacity methods.
 * Unsupported method versions fail closed.
 */
final class SiloSlots {
    private final BlockEntity entity;
    private final @Nullable Method controller;
    private final @Nullable Method capacity;

    SiloSlots(BlockEntity entity) {
        this.entity = entity;
        this.controller = method(entity, "getControllerBE");
        this.capacity = method(entity, "getCapacity");
    }
    private static @Nullable Method method(BlockEntity entity, String name) {
        try {
            return entity.getClass().getMethod(name);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
    /** The controller segment's inventory, or null while the multiblock is unresolved. */
    @Nullable Container target() {
        if (controller == null || capacity == null) return null;
        try {
            Object be = controller.invoke(entity);
            return be instanceof BlockEntity block && be instanceof Container container && !block.isRemoved()
                && block.getClass() == entity.getClass() ? container : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
    boolean active(int slot) {
        Container target = target();
        if (target == null) return false;
        try {
            int count = (Integer) capacity.invoke(target);
            return slot % 81 < count;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
