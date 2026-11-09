package net.notaglitch.bluemap_disguise;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.mca.blockentity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.notaglitch.bluemap_disguise.blockentity.CreateCopycatBlockEntity;
import net.notaglitch.bluemap_disguise.blockentity.MultiStateCopycatBlockEntity;
import net.notaglitch.bluemap_disguise.blockentity.TomsPaintedBlockEntity;
import net.notaglitch.bluemap_disguise.renderer.DisguiseBlockRendererType;
import net.notaglitch.bluemap_disguise.renderer.DisguiseDelegatingRendererType;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod(BlueMapDisguise.MOD_ID)
public class BlueMapDisguise {

    public static final String MOD_ID = "bluemap_disguise";
    private static final AtomicBoolean API_LISTENER_REGISTERED = new AtomicBoolean(false);

    public BlueMapDisguise(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        initializeIntegrations();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(BlueMapDisguise::initializeIntegrations);
    }

    public static void initializeIntegrations() {
        registerBlockEntityTypes();
        registerRenderers();

        // Listen for BlueMap API enablement if loaded as plugin (register once only!)
        if (API_LISTENER_REGISTERED.compareAndSet(false, true)) {
            try {
                BlueMapAPI.onEnable(api -> {
                    registerBlockEntityTypes();
                    registerRenderers();
                });
            } catch (Throwable ignored) {}
        }
    }

    private static void registerBlockEntityTypes() {
        try {
            // 1. Create native copycat
            registerBe("create", "copycat", CreateCopycatBlockEntity.class);
            registerBe("create", "copycat_block_entity", CreateCopycatBlockEntity.class);

            // 2. Copycats+ (single & multi-state)
            registerBe("copycats", "copycat", CreateCopycatBlockEntity.class);
            registerBe("copycats", "copycat_block_entity", CreateCopycatBlockEntity.class);
            registerBe("copycats", "multistate_copycat", MultiStateCopycatBlockEntity.class);

            // 3. Simple Copycats
            registerBe("sim_copycats", "copycat", CreateCopycatBlockEntity.class);
            registerBe("sim_copycats", "copycat_block_entity", CreateCopycatBlockEntity.class);

            // 4. Tom's Simple Storage Mod
            registerBe("toms_storage", "painted", TomsPaintedBlockEntity.class);
            registerBe("toms_storage", "painted_block_entity", TomsPaintedBlockEntity.class);

            Logger.global.logInfo("[BlueMap-Disguise] Disguise BlockEntity types registered to BlueMap.");
        } catch (Throwable t) {
            Logger.global.logWarning("[BlueMap-Disguise] Failed to register block entities: " + t.getMessage());
        }
    }

    private static void registerBe(String namespace, String value, Class<? extends de.bluecolored.bluemap.core.world.BlockEntity> clazz) {
        try {
            BlockEntityType.REGISTRY.register(new BlockEntityType.Impl(new Key(namespace, value), clazz));
        } catch (Throwable ignored) {}
    }

    private static void registerRenderers() {
        try {
            // Register standalone renderer type
            BlockRendererType.REGISTRY.register(new DisguiseBlockRendererType());

            // Wrap DEFAULT renderer so copycats are processed automatically
            DisguiseDelegatingRendererType.wrapDefaultRenderer();
        } catch (Throwable t) {
            Logger.global.logWarning("[BlueMap-Disguise] Failed to register disguise renderers: " + t.getMessage());
        }
    }

}
