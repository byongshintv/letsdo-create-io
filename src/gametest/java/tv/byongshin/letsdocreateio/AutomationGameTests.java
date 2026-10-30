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
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
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
        if (net.minecraftforge.fml.ModList.get().isLoaded("quality_food")) {
            tests.add(test("kettle_quality_food", helper -> QualityFoodTests.kettleGrades(helper)));
        }
        tests.add(test("face_roaster", helper -> {
            var entity = place(helper, "farm_and_charm:roaster");
            Container c = (Container) entity;
            // Owner report 10-29: a bowl fed from the side landed in an ingredient slot. Sides feed the bowl slot only.
            helper.assertTrue(ItemHandlerHelper.insertItem(face(entity, Direction.NORTH), new ItemStack(Items.BOWL), false).isEmpty(), "Side bowl rejected");
            helper.assertTrue(c.getItem(6).is(Items.BOWL), "Side bowl not in container slot");
            helper.assertTrue(ItemHandlerHelper.insertItem(face(entity, Direction.UP), new ItemStack(Items.APPLE), false).isEmpty(), "Top ingredient rejected");
            helper.assertTrue(c.getItem(0).is(Items.APPLE), "Top ingredient not in slot 0");
            // With the bowl slot occupied, a side has nowhere else to put an apple.
            helper.assertTrue(ItemHandlerHelper.insertItem(face(entity, Direction.EAST), new ItemStack(Items.APPLE), false).getCount() == 1,
                "Side apple entered an ingredient slot");
            // The roaster's bottom is on its heat source; the result must also leave through a side.
            c.setItem(7, new ItemStack(Items.BREAD));
            helper.assertTrue(face(entity, Direction.WEST).extractItem(7, 1, false).is(Items.BREAD), "Side extraction of result failed");
            helper.succeed();
        }));
        tests.add(test("face_stove", helper -> {
            var entity = place(helper, "farm_and_charm:stove_block");
            Container c = (Container) entity;
            var side = face(entity, Direction.SOUTH);
            helper.assertTrue(ItemHandlerHelper.insertItem(side, new ItemStack(Items.COAL), false).isEmpty(), "Side fuel rejected");
            helper.assertTrue(c.getItem(4).is(Items.COAL), "Side fuel not in fuel slot");
            ItemStack ingredient = ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new)
                .filter(stack -> !stack.isEmpty() && ForgeHooks.getBurnTime(stack, null) == 0 && c.canPlaceItem(1, stack))
                .findFirst().orElseThrow();
            helper.assertTrue(ItemHandlerHelper.insertItem(side, ingredient.copy(), false).isEmpty(), "Side ingredient rejected");
            helper.assertTrue(ItemStack.isSameItem(c.getItem(1), ingredient), "Side ingredient not in slot 1");
            helper.assertTrue(c.getItem(4).getCount() == 1, "Ingredient entered fuel slot");
            c.setItem(0, new ItemStack(Items.BREAD));
            helper.assertTrue(side.extractItem(0, 1, false).is(Items.BREAD), "Side extraction of result failed");
            helper.assertTrue(ItemHandlerHelper.insertItem(face(entity, Direction.DOWN), new ItemStack(Items.COAL), false).getCount() == 1, "Bottom accepted fuel");
            helper.succeed();
        }));
        for (var water : SupportedBlocks.WATER.entrySet()) {
            tests.add(test("water_" + water.getKey().replace(':', '_'), helper -> {
                var entity = place(helper, water.getKey());
                String key = water.getValue().nbtKey();
                for (Direction side : new Direction[]{Direction.UP, Direction.NORTH, Direction.DOWN, null}) {
                    var tank = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(() -> new AssertionError("No tank " + side));
                    helper.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Accepted lava");
                    helper.assertTrue(tank.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Drained water");
                }
                var tank = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(AssertionError::new);
                helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000, "Simulated fill");
                helper.assertTrue(entity.saveWithoutMetadata().getInt(key) == 0, "Simulation changed level");
                // A bucket's worth equals the native water-bucket step of 25 units.
                helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "Fill bucket");
                helper.assertTrue(entity.saveWithoutMetadata().getInt(key) == 25, "Bucket did not add 25 units");
                // Pipes push small amounts per tick; they must accumulate rather than be refused.
                for (int i = 0; i < 4; i++) tank.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(entity.saveWithoutMetadata().getInt(key) == 26, "Small pushes not accumulated");
                helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 10000), IFluidHandler.FluidAction.EXECUTE) == 4000 - 26 * 40, "Overfilled");
                helper.assertTrue(entity.saveWithoutMetadata().getInt(key) == 100, "Not full");
                helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "Full tank accepted water");
                helper.succeed();
            }));
        }
        tests.add(test("silo_multiblock", helper -> {
            // Owner report 10-29: funnels on a multiblock silo did nothing, because only the controller holds items.
            var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(new ResourceLocation("farm_and_charm:silo"));
            Block block = ForgeRegistries.BLOCKS.getValues().stream().filter(b -> type.isValid(b.defaultBlockState())).findFirst().orElseThrow();
            BlockPos low = new BlockPos(1, 1, 1);
            helper.setBlock(low.below(), Blocks.STONE);
            helper.setBlock(low, block);
            helper.setBlock(low.above(), block);
            helper.succeedWhen(() -> {
                BlockEntity a = helper.getBlockEntity(low), b = helper.getBlockEntity(low.above());
                boolean aController = isController(a);
                helper.assertTrue(aController != isController(b), "Silo multiblock not formed yet");
                BlockEntity segment = aController ? b : a, controller = aController ? a : b;
                var handler = segment.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).orElseThrow(AssertionError::new);
                ItemStack input = ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new)
                    .filter(stack -> !stack.isEmpty() && ((Container) controller).canPlaceItem(0, stack)).findFirst().orElseThrow();
                helper.assertTrue(ItemHandlerHelper.insertItem(handler, input.copy(), false).isEmpty(), "Segment refused input");
                helper.assertTrue(ItemStack.isSameItem(((Container) controller).getItem(0), input), "Input did not reach controller");
                ((Container) controller).setItem(81, new ItemStack(Items.BREAD));
                helper.assertTrue(handler.extractItem(81, 1, false).is(Items.BREAD), "Segment output extraction failed");
                helper.assertTrue(handler.insertItem(2, input.copy(), false).getCount() == input.getCount(), "Slot beyond capacity 2 accepted");
            });
        }));
        tests.add(new TestFunction("automation", "letsdo_create_io.silo_dries_inserted_items", "letsdo_create_io:empty",
            Rotation.NONE, 400, 0, true, helper -> {
            // Owner report 10-29: items inserted by automation seemed not to start drying.
            var entity = place(helper, "farm_and_charm:silo");
            Container c = (Container) entity;
            ItemStack fresh = ForgeRegistries.ITEMS.getValues().stream().map(item -> new ItemStack(item, 16))
                .filter(stack -> !stack.isEmpty() && isDryItem(helper, stack)).findFirst().orElseThrow();
            var handler = face(entity, Direction.UP);
            helper.assertTrue(ItemHandlerHelper.insertItem(handler, fresh.copy(), false).isEmpty(), "Silo refused dry item");
            helper.succeedWhen(() -> {
                helper.assertTrue(!c.getItem(81).isEmpty(), "Inserted item not dried yet");
                helper.assertTrue(c.getItem(0).isEmpty(), "Fresh item left behind");
                helper.assertTrue(handler.extractItem(81, 64, false).getCount() == 16, "Dried output not extractable");
            });
        }));
        tests.add(test("face_meadow_cauldron", helper -> {
            var entity = place(helper, "meadow:cooking_cauldron");
            Container c = (Container) entity;
            var side = face(entity, Direction.NORTH);
            helper.assertTrue(ItemHandlerHelper.insertItem(side, new ItemStack(Items.WATER_BUCKET), false).isEmpty(), "Side water rejected");
            helper.assertTrue(c.getItem(7).is(Items.WATER_BUCKET), "Side water not in fluid slot");
            helper.assertTrue(ItemHandlerHelper.insertItem(side, new ItemStack(Items.APPLE), false).isEmpty(), "Side ingredient rejected");
            helper.assertTrue(c.getItem(1).is(Items.APPLE), "Side ingredient not in slot 1");
            helper.succeed();
        }));
        tests.add(test("face_fermentation_barrel", helper -> {
            var entity = place(helper, "vinery:fermentation_barrel");
            Container c = (Container) entity;
            var worldly = (net.minecraft.world.WorldlyContainer) entity;
            var side = face(entity, Direction.NORTH);
            ItemStack juice = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("nethervinery:crimson_grapejuice")), 2);
            helper.assertTrue(!worldly.canPlaceItemThroughFace(0, juice, Direction.NORTH), "Native side now takes juice; route obsolete");
            helper.assertTrue(ItemHandlerHelper.insertItem(side, juice.copy(), false).isEmpty(), "Side juice rejected");
            helper.assertTrue(ItemStack.isSameItem(c.getItem(0), juice) && c.getItem(0).getCount() == 2, "Side juice not in slot 0");
            ItemStack ingredient = ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new)
                .filter(stack -> !stack.isEmpty() && worldly.canPlaceItemThroughFace(1, stack, Direction.NORTH))
                .findFirst().orElseThrow();
            helper.assertTrue(ItemHandlerHelper.insertItem(side, ingredient.copy(), false).isEmpty(), "Side ingredient rejected");
            helper.assertTrue(ItemStack.isSameItem(c.getItem(1), ingredient), "Side ingredient not in slot 1");
            helper.assertTrue(ItemHandlerHelper.insertItem(side, new ItemStack(Items.DIRT), false).getCount() == 1, "Side accepted non-ingredient");
            helper.assertTrue(ItemHandlerHelper.insertItem(face(entity, Direction.UP), ingredient.copy(), false).getCount() == 1, "Top accepted ingredient");
            helper.succeed();
        }));
        tests.add(test("untargeted_chest", helper -> {
            helper.setBlock(new BlockPos(1, 1, 1), Blocks.CHEST);
            var be = helper.getBlockEntity(new BlockPos(1, 1, 1));
            helper.assertTrue(be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(), "Chest native handler missing");
            helper.assertTrue(!(be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new) instanceof ContainerItemHandler), "Unexpected attachment");
            helper.succeed();
        }));
        return tests;
    }
    private static BlockEntity place(GameTestHelper helper, String typeId) {
        var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(new ResourceLocation(typeId));
        Block block = ForgeRegistries.BLOCKS.getValues().stream().filter(b -> type.isValid(b.defaultBlockState())).findFirst().orElseThrow();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, block);
        return helper.getBlockEntity(pos);
    }
    private static boolean isDryItem(GameTestHelper helper, ItemStack stack) {
        try {
            return (Boolean) Class.forName("net.satisfy.farm_and_charm.block.SiloBlock")
                .getMethod("isDryItem", net.minecraft.world.level.Level.class, ItemStack.class).invoke(null, helper.getLevel(), stack);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
    private static boolean isController(BlockEntity entity) {
        try {
            return (Boolean) entity.getClass().getMethod("isController").invoke(entity);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
    private static IItemHandler face(BlockEntity entity, Direction side) {
        return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(AssertionError::new);
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
            var faced = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(AssertionError::new);
            helper.assertTrue(faced instanceof ContainerItemHandler && faced != handler, id + " missing face handler " + side);
            for (int slot = 0; slot < layout.size(); slot++) {
                if (!layout.canInsert(slot)) {
                    helper.assertTrue(!faced.isItemValid(slot, new ItemStack(Items.APPLE)), id + " face " + side + " opened " + slot);
                }
            }
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
