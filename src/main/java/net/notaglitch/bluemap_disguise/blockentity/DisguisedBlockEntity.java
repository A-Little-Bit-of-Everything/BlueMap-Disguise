package net.notaglitch.bluemap_disguise.blockentity;

import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockState;
import org.jetbrains.annotations.Nullable;

public interface DisguisedBlockEntity extends BlockEntity {

    /**
     * Gets the disguised material BlockState for this block entity.
     *
     * @param face Optional directional face name ("up", "down", "north", "south", "east", "west")
     *             for multi-state copycat blocks, or null for general material.
     * @return The material BlockState, or null if no disguise is present.
     */
    @Nullable BlockState getDisguisedMaterial(@Nullable String face);

}
