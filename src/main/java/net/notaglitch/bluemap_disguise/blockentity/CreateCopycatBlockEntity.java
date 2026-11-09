package net.notaglitch.bluemap_disguise.blockentity;

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;
import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuppressWarnings("FieldMayBeFinal")
public class CreateCopycatBlockEntity extends MCABlockEntity implements DisguisedBlockEntity {

    @NBTName("material")
    private @Nullable BlockState material;

    @Override
    public @Nullable BlockState getDisguisedMaterial(@Nullable String face) {
        return material;
    }

}
