# BlueMap Disguise & Camouflage Addon

A server-side addon mod for **BlueMap** on **NeoForge 1.21.1** that dynamically renders disguised and camouflage blocks on your BlueMap web maps.

## The Technical Problem
BlueMap renders 3D world tiles by reading Minecraft region files (`.mca`) offline. In MCA chunk data, terrain geometry is read solely from the `sections.block_states` palette.

For disguise and camouflage blocks, the world save only records the frame's generic blockstate (e.g. `create:copycat_panel`, `copycats:copycat_slab`, or `toms_storage:painted_trim`). The actual disguised material is stored inside the chunk's `block_entities` list in NBT. Standard BlueMap ignores modded block entity NBT during chunk rendering, causing all disguised blocks to either render as their empty frame or missing textures.

## Supported Mods & Disguises
1. **Create (`create`)**
   - Blocks: `create:copycat_step`, `create:copycat_panel`
   - Block Entity: `create:copycat`
   - Material extracted from NBT `"material"` tag.
2. **Copycats+ (`copycats`)**
   - Blocks: `copycats:copycat_slab`, `copycats:copycat_stairs`, `copycats:copycat_beam`, `copycats:copycat_block`, `copycats:copycat_fence`, `copycats:copycat_wall`, `copycats:copycat_half_panel`, `copycats:copycat_vertical_slice`, `copycats:copycat_board`, `copycats:copycat_byte`, etc.
   - Block Entities: `copycats:copycat` (single material) and `copycats:multistate_copycat` (per-face multi-material).
   - Multi-state face mapping supported (`up`, `down`, `north`, `south`, `east`, `west`).
3. **Simple Copycats (`sim_copycats`)**
   - Blocks: `sim_copycats:absurdly_heavy_copycat`, `sim_copycats:bouncy_copycat`, `sim_copycats:glowing_copycat`, slabs, and stairs.
   - Block Entity: `sim_copycats:copycat`.
4. **Tom's Simple Storage Mod (`toms_storage`)**
   - Blocks: `toms_storage:inventory_cable_framed`, `toms_storage:inventory_cable_connector_framed`, `toms_storage:painted_trim`.
   - Block Entity: `toms_storage:painted`.
   - Material extracted from NBT `"block"` tag.

## How It Works
1. **MCA Block Entity Registration:** Registers custom `MCABlockEntity` deserializers for all supported disguise block entity IDs directly into BlueMap's `BlockEntityType.REGISTRY`. BlueNBT extracts the disguised `BlockState` directly from MCA chunk data.
2. **Dynamic Model & Texture Swapping:** Hooks into BlueMap's `BlockRenderer` pipeline. For full block disguises, it renders the disguised blockstate directly. For shaped blocks (slabs, stairs, panels, steps, cables), it textures the copycat model elements using the disguised material's directional face textures and computes biome tints (grass, leaves, water).

## Building
```bash
./gradlew build
```
Output jar will be located in `build/libs/`.

## License
MIT
