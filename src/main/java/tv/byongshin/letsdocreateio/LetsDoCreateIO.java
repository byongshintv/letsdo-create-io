package tv.byongshin.letsdocreateio;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

@Mod(LetsDoCreateIO.MOD_ID)
public final class LetsDoCreateIO {
    public static final String MOD_ID = "letsdo_create_io";
    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(MOD_ID, "item_handler");
    private static final Map<String, ForgeConfigSpec.BooleanValue> ENABLED = new HashMap<>();
    static final ForgeConfigSpec CONFIG;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.comment("Enable item automation by block entity type. Restart after changing.").push("blocks");
        SupportedBlocks.TABLE.keySet().stream().sorted().forEach(id ->
            ENABLED.put(id, builder.define(id.replace(':', '.'), true)));
        builder.pop();
        CONFIG = builder.build();
    }
    public LetsDoCreateIO() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, EventPriority.LOWEST, this::attach);
    }
    private void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity entity = event.getObject();
        ResourceLocation key = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(entity.getType());
        if (key == null) return;
        String id = key.toString();
        SlotLayout layout = SupportedBlocks.TABLE.get(id);
        if (layout == null || !(entity instanceof Container container) || !ENABLED.get(id).get()) return;
        if (entity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return;
        for (Direction side : Direction.values()) {
            if (entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent()) return;
        }
        // Other attachment listeners' providers are not yet visible through entity.getCapability.
        for (ICapabilityProvider provider : event.getCapabilities().values()) {
            if (provider.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return;
            for (Direction side : Direction.values()) {
                if (provider.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent()) return;
            }
        }
        IntPredicate active = id.equals("farm_and_charm:silo") ? SiloSlots.forEntity(entity) : slot -> true;
        LazyOptional<IItemHandler> handler = LazyOptional.of(() -> new ContainerItemHandler(container, layout,
            () -> !entity.isRemoved() && ENABLED.get(id).get(), active, () -> {
                entity.setChanged();
                var level = entity.getLevel();
                if (level != null && !level.isClientSide) {
                    var state = level.getBlockState(entity.getBlockPos());
                    level.sendBlockUpdated(entity.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
                }
            }));
        event.addCapability(CAPABILITY_ID, new ICapabilityProvider() {
            @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
                return capability == ForgeCapabilities.ITEM_HANDLER ? handler.cast() : LazyOptional.empty();
            }
        });
        event.addListener(handler::invalidate);
    }
}
