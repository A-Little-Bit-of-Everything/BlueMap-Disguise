package net.notaglitch.bluemap_disguise.renderer;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockStateModelRenderer;
import de.bluecolored.bluemap.core.map.hires.block.ResourceModelRenderer;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculator;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.TextureVariable;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.util.math.VectorM2f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.block.ExtendedBlock;
import net.notaglitch.bluemap_disguise.blockentity.DisguisedBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DisguiseBlockRenderer implements BlockRenderer {

    private static final float BLOCK_SCALE = 1f / 16f;

    private final ResourcePack resourcePack;
    private final TextureGallery textureGallery;
    private final RenderSettings renderSettings;
    private final BlockColorCalculator blockColorCalculator;
    private final ResourceModelRenderer delegateRenderer;
    private final BlockStateModelRenderer stateRenderer;

    private final Function<de.bluecolored.bluemap.core.util.Key, Model> modelProvider;
    private final Function<de.bluecolored.bluemap.core.util.Key, Texture> textureProvider;

    private final VectorM3f[] corners = new VectorM3f[8];
    private final VectorM2f[] rawUvs = new VectorM2f[4];
    private final VectorM2f[] uvs = new VectorM2f[4];
    private final Color tintColor = new Color();
    private final Color mapColor = new Color();
    private final VectorM3f faceRotationVector = new VectorM3f(0, 0, 0);
    private final MatrixM4f modelElementTransform = new MatrixM4f();

    private record MaterialFaceKey(BlockState material, @Nullable Direction face) {}
    private final Map<MaterialFaceKey, ResourcePath<Texture>> textureCache = new ConcurrentHashMap<>();

    private BlockNeighborhood currentBlock;
    private Variant currentVariant;
    private Model currentCopycatModel;
    private TileModelView currentBlockModel;
    private Color currentBlockColor;
    private float currentBlockColorOpacity;
    private DisguisedBlockEntity currentDisguisedBe;

    public DisguiseBlockRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.textureGallery = textureGallery;
        this.renderSettings = renderSettings;
        this.blockColorCalculator = resourcePack.createBlockColorCalculator();
        this.delegateRenderer = new ResourceModelRenderer(resourcePack, textureGallery, renderSettings);
        this.stateRenderer = new BlockStateModelRenderer(resourcePack, textureGallery, renderSettings);

        this.modelProvider = resourcePack.getModels()::get;
        this.textureProvider = resourcePack.getTextures()::get;

        for (int i = 0; i < corners.length; i++) corners[i] = new VectorM3f(0, 0, 0);
        for (int i = 0; i < rawUvs.length; i++) rawUvs[i] = new VectorM2f(0, 0);
    }

    @Override
    public void render(BlockNeighborhood block, Variant variant, TileModelView blockModel, Color color) {
        BlockEntity be = block.getBlockEntity();
        if (!(be instanceof DisguisedBlockEntity dbe)) {
            delegateRenderer.render(block, variant, blockModel, color);
            return;
        }

        BlockState defaultMaterial = dbe.getDisguisedMaterial(null);
        if (defaultMaterial == null || defaultMaterial.isAir() || isBaseBlock(defaultMaterial)) {
            delegateRenderer.render(block, variant, blockModel, color);
            return;
        }

        // If the copycat is a full solid block (e.g. toms_storage:painted_trim or full copycat block),
        // we can directly render the material's blockstate models.
        if (isFullBlockDisguise(block.getBlockState())) {
            stateRenderer.render(block, defaultMaterial, blockModel, color);
            return;
        }

        // Shaped disguise (step, panel, slab, stair, beam, fence, cable):
        // Render the copycat model elements using the disguised material's textures & tints.
        renderShapedDisguise(block, variant, blockModel, color, dbe, defaultMaterial);
    }

    private boolean isBaseBlock(BlockState state) {
        String id = state.getId().getFormatted();
        return id.contains("copycat_base") || id.equals("minecraft:air");
    }

    private boolean isFullBlockDisguise(BlockState state) {
        String id = state.getId().getFormatted();
        return id.equals("toms_storage:painted_trim")
                || id.equals("copycats:copycat_block")
                || id.contains("copycat_full");
    }

    private void renderShapedDisguise(
            BlockNeighborhood block,
            Variant variant,
            TileModelView blockModel,
            Color color,
            DisguisedBlockEntity dbe,
            BlockState defaultMaterial
    ) {
        this.currentBlock = block;
        this.currentVariant = variant;
        this.currentBlockModel = blockModel;
        this.currentBlockColor = color;
        this.currentBlockColorOpacity = 0f;
        this.currentDisguisedBe = dbe;
        this.currentCopycatModel = variant.getModel().getResource(modelProvider);

        if (this.currentCopycatModel == null) {
            delegateRenderer.render(block, variant, blockModel, color);
            return;
        }

        this.tintColor.set(0, 0, 0, -1, true);

        int modelStart = blockModel.getStart();
        Element[] elements = currentCopycatModel.getElements();
        if (elements != null) {
            for (Element element : elements) {
                buildDisguisedElement(element, blockModel.initialize(), defaultMaterial);
            }
        }

        if (color.a > 0) {
            color.flatten().straight();
            color.a = currentBlockColorOpacity;
        }

        blockModel.initialize(modelStart);

        if (variant.isTransformed())
            blockModel.transform(variant.getTransformMatrix());
    }

    private void buildDisguisedElement(Element element, TileModelView blockModel, BlockState defaultMaterial) {
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();

        float minX = from.getX(), minY = from.getY(), minZ = from.getZ();
        float maxX = to.getX(), maxY = to.getY(), maxZ = to.getZ();

        VectorM3f[] c = corners;
        c[0].x = minX; c[0].y = minY; c[0].z = minZ;
        c[1].x = minX; c[1].y = minY; c[1].z = maxZ;
        c[2].x = maxX; c[2].y = minY; c[2].z = minZ;
        c[3].x = maxX; c[3].y = minY; c[3].z = maxZ;
        c[4].x = minX; c[4].y = maxY; c[4].z = minZ;
        c[5].x = minX; c[5].y = maxY; c[5].z = maxZ;
        c[6].x = maxX; c[6].y = maxY; c[6].z = minZ;
        c[7].x = maxX; c[7].y = maxY; c[7].z = maxZ;

        int modelStart = blockModel.getStart();
        createDisguisedFace(element, Direction.DOWN, c[0], c[2], c[3], c[1], defaultMaterial);
        createDisguisedFace(element, Direction.UP, c[5], c[7], c[6], c[4], defaultMaterial);
        createDisguisedFace(element, Direction.NORTH, c[2], c[0], c[4], c[6], defaultMaterial);
        createDisguisedFace(element, Direction.SOUTH, c[1], c[3], c[7], c[5], defaultMaterial);
        createDisguisedFace(element, Direction.WEST, c[0], c[1], c[5], c[4], defaultMaterial);
        createDisguisedFace(element, Direction.EAST, c[3], c[2], c[6], c[7], defaultMaterial);
        blockModel.initialize(modelStart);

        blockModel.transform(modelElementTransform
                .copy(element.getRotation().getMatrix())
                .scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE)
        );
    }

    private void createDisguisedFace(
            Element element,
            Direction faceDir,
            VectorM3f c0, VectorM3f c1, VectorM3f c2, VectorM3f c3,
            BlockState defaultMaterial
    ) {
        Face face = element.getFaces().get(faceDir);
        if (face == null) return;

        Vector3i faceDirVector = faceDir.toVector();

        ExtendedBlock facedBlockNeighbor = getRotationRelativeBlock(faceDir);
        LightData blockLightData = currentBlock.getLightData();
        LightData facedLightData = facedBlockNeighbor.getLightData();

        int sunLight = Math.max(blockLightData.getSkyLight(), facedLightData.getSkyLight());
        int blockLight = Math.max(blockLightData.getBlockLight(), facedLightData.getBlockLight());

        if (currentBlock.isRemoveIfCave() &&
                (renderSettings.isCaveDetectionUsesBlockLight() ? Math.max(blockLight, sunLight) : sunLight) == 0
        ) return;

        faceRotationVector.set(faceDirVector);
        faceRotationVector.rotateAndScale(element.getRotation().getMatrix());
        makeRotationRelative(faceRotationVector);

        if (renderSettings.isRenderTopOnly() && faceRotationVector.y < 0.01) return;
        if (face.getCullface() != null) {
            ExtendedBlock b = getRotationRelativeBlock(face.getCullface());
            BlockProperties p = b.getProperties();
            if (p.isCulling()) return;
            if (p.getCullingIdentical() && b.getBlockState().equals(currentBlock.getBlockState())) return;
        }

        // Determine face material (support multi-state copycat face materials)
        String faceName = faceDir.name().toLowerCase();
        BlockState faceMaterial = currentDisguisedBe.getDisguisedMaterial(faceName);
        if (faceMaterial == null || faceMaterial.isAir() || isBaseBlock(faceMaterial)) {
            faceMaterial = defaultMaterial;
        }

        // Resolve face texture from the material
        ResourcePath<Texture> texturePath = resolveMaterialTexture(faceMaterial, faceDir);
        if (texturePath == null && faceMaterial != defaultMaterial) {
            texturePath = resolveMaterialTexture(defaultMaterial, faceDir);
        }
        if (texturePath == null) {
            // fallback to copycat's own texture
            texturePath = face.getTexture().getTexturePath(key -> currentCopycatModel.getTextures().get(key));
        }

        currentBlockModel.initialize();
        currentBlockModel.add(2);

        TileModel tileModel = currentBlockModel.getTileModel();
        int face1 = currentBlockModel.getStart();
        int face2 = face1 + 1;

        // Positions
        tileModel.setPositions(face1,
                c0.x, c0.y, c0.z,
                c1.x, c1.y, c1.z,
                c2.x, c2.y, c2.z
        );
        tileModel.setPositions(face2,
                c0.x, c0.y, c0.z,
                c2.x, c2.y, c2.z,
                c3.x, c3.y, c3.z
        );

        // Texture
        int textureId = textureGallery.get(texturePath);
        tileModel.setMaterialIndex(face1, textureId);
        tileModel.setMaterialIndex(face2, textureId);

        // UVs
        Vector4f uvRaw = face.getUv();
        float uvx = uvRaw.getX() / 16f,
                uvy = uvRaw.getY() / 16f,
                uvz = uvRaw.getZ() / 16f,
                uvw = uvRaw.getW() / 16f;

        rawUvs[0].set(uvx, uvw);
        rawUvs[1].set(uvz, uvw);
        rawUvs[2].set(uvz, uvy);
        rawUvs[3].set(uvx, uvy);

        int rotationSteps = Math.floorDiv(face.getRotation(), 90) % 4;
        if (rotationSteps < 0) rotationSteps += 4;
        for (int i = 0; i < 4; i++)
            uvs[i] = rawUvs[(rotationSteps + i) % 4];

        tileModel.setUvs(face1,
                uvs[0].x, uvs[0].y,
                uvs[1].x, uvs[1].y,
                uvs[2].x, uvs[2].y
        );
        tileModel.setUvs(face2,
                uvs[0].x, uvs[0].y,
                uvs[2].x, uvs[2].y,
                uvs[3].x, uvs[3].y
        );

        // Tint calculation from disguised material
        if (face.getTintindex() >= 0 || requiresMaterialTint(faceMaterial)) {
            blockColorCalculator.getBlockColor(currentBlock, faceMaterial, tintColor);
            tileModel.setColor(face1, tintColor.r, tintColor.g, tintColor.b);
            tileModel.setColor(face2, tintColor.r, tintColor.g, tintColor.b);
        } else {
            tileModel.setColor(face1, 1f, 1f, 1f);
            tileModel.setColor(face2, 1f, 1f, 1f);
        }

        // Lighting
        int emissiveBlockLight = Math.max(blockLight, element.getLightEmission());
        tileModel.setBlocklight(face1, emissiveBlockLight);
        tileModel.setBlocklight(face2, emissiveBlockLight);

        tileModel.setSunlight(face1, sunLight);
        tileModel.setSunlight(face2, sunLight);

        // AO
        tileModel.setAOs(face1, 1f, 1f, 1f);
        tileModel.setAOs(face2, 1f, 1f, 1f);

        // Color opacity calculation for map
        float a = faceRotationVector.y;
        if (a > 0.01 && texturePath != null) {
            Texture texture = texturePath.getResource(textureProvider);
            if (texture != null) {
                mapColor.set(texture.getColorPremultiplied());
                if (tintColor.a >= 0) mapColor.multiply(tintColor);
                float combinedLight = Math.max(sunLight / 15f, blockLight / 15f);
                combinedLight = (1 - renderSettings.getAmbientLight()) * combinedLight + renderSettings.getAmbientLight();
                mapColor.r *= combinedLight;
                mapColor.g *= combinedLight;
                mapColor.b *= combinedLight;
                if (mapColor.a > currentBlockColorOpacity) currentBlockColorOpacity = mapColor.a;
            }
        }
    }

    private @Nullable ResourcePath<Texture> resolveMaterialTexture(BlockState material, @Nullable Direction faceDir) {
        MaterialFaceKey cacheKey = new MaterialFaceKey(material, faceDir);
        ResourcePath<Texture> cached = textureCache.get(cacheKey);
        if (cached != null) return cached;

        var materialStateResource = resourcePack.getBlockState(material);
        if (materialStateResource == null) return null;

        List<Variant> materialVariants = new ArrayList<>();
        materialStateResource.forEach(material, 0, 0, 0, materialVariants::add);
        if (materialVariants.isEmpty()) return null;

        for (Variant matVariant : materialVariants) {
            Model matModel = matVariant.getModel().getResource(modelProvider);
            if (matModel == null) continue;

            Map<String, TextureVariable> textures = matModel.getTextures();
            if (textures == null || textures.isEmpty()) continue;

            // Preferred directional names
            String[] preferredKeys;
            if (faceDir == Direction.UP) preferredKeys = new String[]{"top", "up", "end", "all", "texture", "particle"};
            else if (faceDir == Direction.DOWN) preferredKeys = new String[]{"bottom", "down", "end", "all", "texture", "particle"};
            else preferredKeys = new String[]{"side", "front", "north", "south", "east", "west", "all", "texture", "particle"};

            TextureVariable selectedVar = null;
            for (String key : preferredKeys) {
                selectedVar = textures.get(key);
                if (selectedVar != null) break;
            }

            if (selectedVar == null && faceDir != null) {
                selectedVar = textures.get(faceDir.name().toLowerCase());
            }

            if (selectedVar == null) {
                Element[] elements = matModel.getElements();
                if (elements != null) {
                    for (Element el : elements) {
                        Face face = faceDir != null ? el.getFaces().get(faceDir) : null;
                        if (face != null) {
                            selectedVar = face.getTexture();
                            if (selectedVar != null) break;
                        }
                    }
                }
            }

            if (selectedVar == null) {
                selectedVar = textures.values().iterator().next();
            }

            ResourcePath<Texture> path = selectedVar.getTexturePath(textures::get);
            if (path != null && !path.getFormatted().contains("copycat_base")) {
                textureCache.put(cacheKey, path);
                return path;
            }
        }
        return null;
    }

    private boolean requiresMaterialTint(BlockState state) {
        String id = state.getId().getFormatted();
        return id.contains("grass") || id.contains("leaves") || id.contains("water") || id.contains("vine");
    }

    private final VectorM3f rotationRelativeBlockDirection = new VectorM3f(0, 0, 0);
    private ExtendedBlock getRotationRelativeBlock(Direction direction) {
        Vector3i v = direction.toVector();
        rotationRelativeBlockDirection.set(v.getX(), v.getY(), v.getZ());
        makeRotationRelative(rotationRelativeBlockDirection);
        return currentBlock.getNeighborBlock(
                Math.round(rotationRelativeBlockDirection.x),
                Math.round(rotationRelativeBlockDirection.y),
                Math.round(rotationRelativeBlockDirection.z)
        );
    }

    private void makeRotationRelative(VectorM3f direction) {
        if (currentVariant.isTransformed()) {
            direction.rotateAndScale(currentVariant.getTransformMatrix());
        }
    }

}
