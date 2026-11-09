package net.notaglitch.bluemap_disguise.renderer;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;

public class DisguiseBlockRendererType implements BlockRendererType {

    public static final Key KEY = new Key("bluemap_disguise", "disguise");

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public BlockRenderer create(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        return new DisguiseBlockRenderer(resourcePack, textureGallery, renderSettings);
    }

    @Override
    public boolean isFallbackFor(BlockState blockState) {
        String id = blockState.getId().getFormatted();
        return id.contains("copycat")
                || id.startsWith("create:copycat")
                || id.startsWith("copycats:copycat")
                || id.startsWith("sim_copycats:")
                || id.startsWith("toms_storage:");
    }

}
