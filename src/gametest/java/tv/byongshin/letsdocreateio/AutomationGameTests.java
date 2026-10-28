package tv.byongshin.letsdocreateio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(LetsDoCreateIO.MOD_ID)
public final class AutomationGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        SupportedBlocks.TABLE.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> {
            var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(new ResourceLocation(entry.getKey()));
            if (type == null) throw new IllegalStateException("Missing tested type: " + entry.getKey());
            int count = 0;
            for (Block block : ForgeRegistries.BLOCKS.getValues()) {
                if (!type.isValid(block.defaultBlockState())) continue;
                String blockId = ForgeRegistries.BLOCKS.getKey(block).toString();
                tests.add(test(blockId.replace(':', '_'), helper -> checkBlock(helper, block, entry.getKey(), entry.getValue())));
                count++;
            }
            if (count == 0) throw new IllegalStateException("No block for " + entry.getKey());
        });
        for (String nativeId : List.of("farm_and_charm:mincer", "farm_and_charm:crafting_bowl",
                "meadow:stove_block_entity", "herbalbrews:stove_block_entity")) {
            var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(new ResourceLocation(nativeId));
            if (type == null) throw new IllegalStateException("Missing native type: " + nativeId);
            for (Block block : ForgeRegistries.BLOCKS.getValues()) {
                if (!type.isValid(block.defaultBlockState())) continue;
                tests.add(test("native_" + ForgeRegistries.BLOCKS.getKey(block).toString().replace(':', '_'), helper -> {
                    BlockPos pos = new BlockPos(1, 1, 1);
                    helper.setBlock(pos.below(), Blocks.STONE);
                    helper.setBlock(pos, block);
                    var entity = helper.getBlockEntity(pos);
                    var handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
                    helper.assertTrue(!(handler instanceof ContainerItemHandler), "Replaced native handler: " + nativeId);
                    helper.succeed();
                }));
            }
        }
        tests.add(test("handler_contract", AutomationGameTests::handlerContract));
        tests.add(test("untargeted_chest", helper -> {
            helper.setBlock(new BlockPos(1, 1, 1), Blocks.CHEST);
            var be = helper.getBlockEntity(new BlockPos(1, 1, 1));
            helper.assertTrue(be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(), "Chest native handler missing");
            helper.assertTrue(!(be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new) instanceof ContainerItemHandler), "Unexpected attachment");
            helper.succeed();
        }));
        return tests;
    }
    private static TestFunction test(String name, java.util.function.Consumer<GameTestHelper> body) {
        return new TestFunction("automation", "letsdo_create_io." + name, "letsdo_create_io:empty",
            Rotation.NONE, 100, 0, true, body);
    }
    private static void checkBlock(GameTestHelper helper, Block block, String id, SlotLayout layout) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, block);
        BlockEntity entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity instanceof Container, id + " must implement Container");
        helper.assertTrue(id.equals(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(entity.getType()).toString()), "Wrong type");
        Container container = (Container) entity;
        IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(() -> new AssertionError(id + " missing capability"));
        helper.assertTrue(handler instanceof ContainerItemHandler, id + " must use attached handler");
        helper.assertTrue(handler.getSlots() == container.getContainerSize(), id + " size mismatch");
        for (Direction side : Direction.values()) {
            helper.assertTrue(entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(AssertionError::new) == handler, id + " differs by side");
        }
        for (int slot = 0; slot < layout.size(); slot++) {
            if (handler.getSlotLimit(slot) == 0) {
                helper.assertTrue(handler.insertItem(slot, new ItemStack(Items.APPLE), false).getCount() == 1, "Inactive silo slot accepted item");
                continue;
            }
            if (layout.canInsert(slot)) {
                final int inputSlot = slot;
                ItemStack item = new ItemStack(Items.APPLE, 2);
                if (!container.canPlaceItem(slot, item)) {
                    item = ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new)
                        .filter(stack -> !stack.isEmpty() && container.canPlaceItem(inputSlot, stack)).findFirst().orElseThrow();
                }
                int original = item.getCount();
                helper.assertTrue(handler.insertItem(slot, item, true).isEmpty(), id + " simulated insert " + slot);
                helper.assertTrue(container.getItem(slot).isEmpty(), "Simulation mutated inventory");
                helper.assertTrue(handler.insertItem(slot, item, false).isEmpty(), id + " insert " + slot);
                helper.assertTrue(item.getCount() == original, "Insert mutated argument");
                helper.assertTrue(container.getItem(slot).getCount() == original, "Insert lost item");
                helper.assertTrue(handler.extractItem(slot, 1, false).isEmpty(), id + " extracted input " + slot);
            } else if (layout.canExtract(slot)) {
                helper.assertTrue(handler.insertItem(slot, new ItemStack(Items.APPLE), false).getCount() == 1, id + " inserted into output " + slot);
                container.setItem(slot, new ItemStack(Items.APPLE, 2));
                helper.assertTrue(handler.extractItem(slot, 1, true).getCount() == 1, "Simulated extract failed");
                helper.assertTrue(container.getItem(slot).getCount() == 2, "Simulation extracted item");
                helper.assertTrue(handler.extractItem(slot, 1, false).getCount() == 1, id + " extract " + slot);
                helper.assertTrue(container.getItem(slot).getCount() == 1, "Wrong output remainder");
            }
            container.setItem(slot, ItemStack.EMPTY);
        }
        var optional = entity.getCapability(ForgeCapabilities.ITEM_HANDLER);
        entity.invalidateCaps();
        helper.assertTrue(!optional.isPresent(), "Capability not invalidated");
        helper.succeed();
    }
    private static void handlerContract(GameTestHelper helper) {
        int[] changes = {0};
        boolean[] enabled = {true};
        var container = new SimpleContainer(2) {
            @Override public int getMaxStackSize() { return 8; }
            @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.is(Items.APPLE); }
        };
        var handler = new ContainerItemHandler(container, new SlotLayout(2, List.of(0), List.of(), List.of(1)),
            () -> enabled[0], slot -> true, () -> changes[0]++);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1, "canPlaceItem ignored");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.APPLE, 12), false).getCount() == 4, "Container limit ignored");
        helper.assertTrue(changes[0] == 1, "Missing mutation callback");
        handler.getStackInSlot(0).setCount(1);
        helper.assertTrue(container.getItem(0).getCount() == 8, "Returned mutable inventory reference");
        container.setItem(0, ItemStack.EMPTY);
        ItemStack tagged = new ItemStack(Items.APPLE);
        tagged.getOrCreateTag().putString("test", "different");
        handler.insertItem(0, tagged, false);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.APPLE), false).getCount() == 1, "Merged different NBT");
        enabled[0] = false;
        helper.assertTrue(handler.insertItem(0, tagged, false).getCount() == 1, "Disabled handler accepted item");
        helper.assertTrue(handler.extractItem(1, 1, false).isEmpty(), "Disabled handler extracted item");
        helper.succeed();
    }
}
