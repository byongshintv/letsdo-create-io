package tv.byongshin.letsdocreateio;

import java.lang.reflect.Method;
import java.util.function.IntPredicate;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The only reflective adapter: the silo exposes capacity outside Container. */
final class SiloSlots {
    private SiloSlots() {}
    static IntPredicate forEntity(BlockEntity entity) {
        try {
            Method capacity = entity.getClass().getMethod("getCapacity");
            return slot -> {
                try {
                    int count = (Integer) capacity.invoke(entity);
                    return slot % 81 < count;
                } catch (ReflectiveOperationException e) {
                    return false;
                }
            };
        } catch (ReflectiveOperationException e) {
            return slot -> false;
        }
    }
}
