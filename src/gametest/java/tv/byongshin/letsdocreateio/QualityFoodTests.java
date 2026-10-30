package tv.byongshin.letsdocreateio;

import de.cadentem.quality_food.capability.BlockDataProvider;
import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

/** Owner report 10-30: automated oolong tea never got a star although its dried leaves had one. */
final class QualityFoodTests {
    private QualityFoodTests() {}

    static void kettleGrades(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("herbalbrews:tea_kettle")));
        var entity = helper.getBlockEntity(pos);
        Container container = (Container) entity;
        var handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        var data = BlockDataProvider.getCapability(entity).orElseThrow(AssertionError::new);
        Item tea = ForgeRegistries.ITEMS.getValue(new ResourceLocation("herbalbrews:oolong_tea"));

        // Entries left by outputs that were extracted before this fix must not decide the current output's grade.
        for (int i = 0; i < 4; i++) data.addQualityEntry(Quality.NONE, 0);
        for (int i = 0; i < 2; i++) data.addQualityEntry(Quality.values()[1], 0);
        container.setItem(0, new ItemStack(tea, 2));

        helper.assertTrue(QualityUtils.getQuality(handler.extractItem(0, 2, true)) == Quality.NONE, "Simulation applied a grade");
        helper.assertTrue(data.getQueueSize() == 6, "Simulation consumed the queue");
        ItemStack result = handler.extractItem(0, 2, false);
        helper.assertTrue(result.getCount() == 2 && QualityUtils.getQuality(result) != Quality.NONE, "Extracted tea has no grade");
        helper.assertTrue(data.getQueueSize() == 0, "Queue not consumed");
        helper.assertTrue(container.getItem(0).isEmpty(), "Output slot not emptied");

        // An ungraded brew stays ungraded unless Quality Food's own roll raises it; extraction still works with no queue.
        container.setItem(0, new ItemStack(tea, 1));
        helper.assertTrue(handler.extractItem(0, 1, false).is(tea), "Extraction without a queue failed");
        helper.succeed();
    }
}
