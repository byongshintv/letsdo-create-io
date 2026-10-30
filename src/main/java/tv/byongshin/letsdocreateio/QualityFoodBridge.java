package tv.byongshin.letsdocreateio;

import de.cadentem.quality_food.capability.BlockDataProvider;
import de.cadentem.quality_food.util.Utils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Quality Food records a kettle's grades per brew and applies them only on its own GUI shift-click path, so handler
 * extraction must apply them too. Loaded only when Quality Food is present.
 */
final class QualityFoodBridge {
    private QualityFoodBridge() {}

    /** {@code available} is the output slot count before extraction; older queue entries belong to items already gone. */
    static void applyCookingQuality(BlockEntity entity, ItemStack result, int available) {
        BlockDataProvider.getCapability(entity).ifPresent(data -> {
            int stale = (int) data.getQueueSize() - available;
            if (stale > 0) Utils.useQuality(entity, result.copyWithCount(stale), null);
            Utils.useQuality(entity, result, null);
        });
    }
}
