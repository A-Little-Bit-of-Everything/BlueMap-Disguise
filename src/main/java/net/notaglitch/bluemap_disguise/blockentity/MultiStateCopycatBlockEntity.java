package net.notaglitch.bluemap_disguise.blockentity;

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;
import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuppressWarnings("FieldMayBeFinal")
public class MultiStateCopycatBlockEntity extends MCABlockEntity implements DisguisedBlockEntity {

    @NBTName("material")
    private @Nullable BlockState material;

    @NBTName("materials")
    private @Nullable Map<String, BlockState> materials;

    @Override
    public @Nullable BlockState getDisguisedMaterial(@Nullable String face) {
        if (materials != null && face != null) {
            BlockState faceState = materials.get(face.toLowerCase());
            if (faceState != null) return faceState;
        }
        return material;
    }

}
