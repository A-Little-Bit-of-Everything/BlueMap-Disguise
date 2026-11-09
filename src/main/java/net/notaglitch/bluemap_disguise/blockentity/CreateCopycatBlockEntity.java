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
public class CreateCopycatBlockEntity extends MCABlockEntity implements DisguisedBlockEntity {

    @NBTName("material")
    private @Nullable BlockState material;

    @NBTName("Material")
    private @Nullable BlockState materialUpper;

    @NBTName("materials")
    private @Nullable Map<String, BlockState> materials;

    @NBTName("material_data")
    private @Nullable Map<String, MaterialWrapper> materialData;

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class MaterialWrapper {
        @NBTName("material")
        private @Nullable BlockState material;

        @NBTName("Material")
        private @Nullable BlockState materialUpper;

        @NBTName("consumedItem")
        private @Nullable ConsumedItem consumedItem;

        public @Nullable BlockState getMaterial() {
            if (material != null && !isBase(material)) return material;
            if (materialUpper != null && !isBase(materialUpper)) return materialUpper;
            if (consumedItem != null && consumedItem.getId() != null && !consumedItem.getId().isEmpty()) {
                String id = consumedItem.getId();
                if (!id.equals("minecraft:air") && !id.contains("copycat_base")) {
                    try {
                        return new BlockState(de.bluecolored.bluemap.core.util.Key.parse(id));
                    } catch (Throwable ignored) {}
                }
            }
            return null;
        }

        @Getter
        @EqualsAndHashCode
        @ToString
        public static class ConsumedItem {
            @NBTName("id")
            private @Nullable String id;
        }
    }

    @Override
    public @Nullable BlockState getDisguisedMaterial(@Nullable String face) {
        // 1. Try face-specific material if face requested
        if (face != null) {
            String f = face.toLowerCase();
            if (materialData != null) {
                MaterialWrapper wrapper = materialData.get(f);
                if (wrapper != null && !isBase(wrapper.getMaterial())) {
                    return wrapper.getMaterial();
                }
            }
            if (materials != null) {
                BlockState state = materials.get(f);
                if (!isBase(state)) {
                    return state;
                }
            }
        }

        // 2. Direct material tags
        if (!isBase(material)) return material;
        if (!isBase(materialUpper)) return materialUpper;

        // 3. Fallback: Search any non-base material in materialData
        if (materialData != null) {
            for (MaterialWrapper wrapper : materialData.values()) {
                if (wrapper != null && !isBase(wrapper.getMaterial())) {
                    return wrapper.getMaterial();
                }
            }
        }

        // 4. Fallback: Search any non-base material in materials
        if (materials != null) {
            for (BlockState state : materials.values()) {
                if (!isBase(state)) {
                    return state;
                }
            }
        }

        return null;
    }

    protected static boolean isBase(@Nullable BlockState state) {
        if (state == null) return true;
        String id = state.getId().getFormatted();
        return id.equals("minecraft:air")
                || id.equals("create:copycat_base")
                || id.equals("copycats:copycat_base")
                || id.contains("copycat_base");
    }

}
