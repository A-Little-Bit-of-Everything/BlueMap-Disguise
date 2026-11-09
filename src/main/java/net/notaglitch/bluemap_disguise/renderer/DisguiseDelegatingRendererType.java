package net.notaglitch.bluemap_disguise.renderer;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import net.notaglitch.bluemap_disguise.blockentity.DisguisedBlockEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseDelegatingRendererType implements BlockRendererType {

    private final BlockRendererType delegate;

    public DisguiseDelegatingRendererType(BlockRendererType delegate) {
        this.delegate = delegate;
    }

    public BlockRendererType getDelegate() {
        return delegate;
    }

    @Override
    public Key getKey() {
        return delegate.getKey();
    }

    @Override
    public boolean isFallbackFor(BlockState blockState) {
        return delegate.isFallbackFor(blockState);
    }

    @Override
    public BlockRenderer create(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        BlockRenderer delegateRenderer = delegate.create(resourcePack, textureGallery, renderSettings);
        DisguiseBlockRenderer disguiseRenderer = new DisguiseBlockRenderer(resourcePack, textureGallery, renderSettings);

        return new BlockRenderer() {
            @Override
            public void render(BlockNeighborhood block, Variant variant, TileModelView tileModel, Color blockColor) {
                BlockEntity be = block.getBlockEntity();
                if (be instanceof DisguisedBlockEntity) {
                    disguiseRenderer.render(block, variant, tileModel, blockColor);
                } else {
                    delegateRenderer.render(block, variant, tileModel, blockColor);
                }
            }
        };
    }

    public static void wrapDefaultRenderer() {
        try {
            Field entriesField = Registry.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Key, BlockRendererType> map =
                    (ConcurrentHashMap<Key, BlockRendererType>) entriesField.get(BlockRendererType.REGISTRY);

            Key defaultKey = Key.bluemap("default");
            BlockRendererType existingDefault = map.get(defaultKey);
            if (existingDefault != null && !(existingDefault instanceof DisguiseDelegatingRendererType)) {
                if (existingDefault.getClass().getSimpleName().equals("CTMDelegatingRendererType")) {
                    try {
                        Method getDelegate = existingDefault.getClass().getMethod("getDelegate");
                        BlockRendererType inner = (BlockRendererType) getDelegate.invoke(existingDefault);
                        if (!(inner instanceof DisguiseDelegatingRendererType)) {
                            BlockRendererType wrapped = new DisguiseDelegatingRendererType(inner);
                            Constructor<?> ctor = existingDefault.getClass().getConstructor(BlockRendererType.class);
                            map.put(defaultKey, (BlockRendererType) ctor.newInstance(wrapped));
                            Logger.global.logInfo("[BlueMap-Disguise] Successfully hooked under BlueMap-CTM DEFAULT BlockRenderer.");
                            return;
                        }
                    } catch (Throwable ignored) {}
                }
                map.put(defaultKey, new DisguiseDelegatingRendererType(existingDefault));
                Logger.global.logInfo("[BlueMap-Disguise] Successfully hooked into BlueMap DEFAULT BlockRenderer.");
            }
        } catch (Throwable t) {
            Logger.global.logWarning("[BlueMap-Disguise] Could not hook into DEFAULT BlockRenderer directly: " + t.getMessage());
        }
    }

}
