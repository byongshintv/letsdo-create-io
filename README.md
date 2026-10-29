# Let's Do Create IO

A server-side Forge **1.20.1 / 47.4.0**, Java 17 addon that exposes selected
Let's Do processing inventories through Forge `IItemHandler`. Create funnels,
chutes, packagers and other capability-based automation can insert ingredients
and extract products. No blocks, items, textures, recipes, or networking channels
are added. No Let's Do code or assets are redistributed.

## Installation

Install the release jar in the **server's** `mods` directory alongside the desired
Let's Do mods and their dependencies. Clients do not need this addon; client
installation is optional. Clients still need the underlying content mods required
by the server. `displayTest=IGNORE_ALL_VERSION` permits addon version/absence
mismatches. All Let's Do dependencies of this addon are optional.

The adapter uses registry ID strings and Minecraft `Container`; it does not link
against Let's Do classes. Blocks already exposing an item handler, on any face,
are left untouched. Untargeted blocks and storage furniture are left untouched.
Vanilla hoppers may use a block's existing `WorldlyContainer` interface directly;
this addon controls the **Forge capability** route, not vanilla sided inventory
rules. Manual stirring, cranking, heating and multiblock assembly remain necessary
where the original mod requires them.

## Supported blocks

Indices below are the original, **zero-based Container slots**, not GUI positions.
Inputs and fuel are insertion-only; outputs are extraction-only. Insertion
respects `Container.canPlaceItem`, container capacity, item stack size, and stack
NBT. Simulation never changes the inventory; actual changes call `setChanged` and
send a block update.

### Faces

Each face gets its own handler:

- **Insertion** follows the block's own `WorldlyContainer` face rules
  (`getSlotsForFace` and `canPlaceItemThroughFace`), the same layout the original
  mod designed for vanilla hoppers. For example, pots, the pan and the roaster take
  ingredients from the top and serving containers (bowls) from the sides.
- **Extraction** of output slots works from **every face**, because a pot's or
  roaster's bottom usually rests on its heat source and a stove's top holds a pot.
- **Covered native faces.** Two cases have an input whose only native face is the
  one a required neighbour covers. Their horizontal faces route by item instead:
  - Stoves (`farm_and_charm`, `candlelight`): fuel goes to slot 4, anything else to
    ingredients 1–3. Natively ingredients enter only from the top.
  - Meadow cooking cauldron: `meadow:small_water_fill` / `large_water_fill` items go
    to slot 7, anything else to 1–6. Natively water enters only from below, where
    the heat source must be.
- Blocks without native face rules (silo, brewing station) expose the same slots on
  every face.
- The unsided (`null`) capability exposes every table slot.

| Block entity registry ID | Blocks / variants | Input slots | Fuel slots | Output slots |
| --- | --- | --- | --- | --- |
| `bakery:small_cooking_pot` | Small cooking pot | 0–6 | — | 7 |
| `beachparty:tiki_bar` | Tiki bar | 1, 2 | — | 0 |
| `beachparty:mini_fridge` | Mini fridge (processes recipes) | 1, 2 | — | 0 |
| `brewery:brewingstation` | Wooden, copper, netherite brewing stations | 0–2 | — | None¹ |
| `candlelight:cooking_pot` | Large cooking pot | 0–6 | — | 7 |
| `candlelight:cooking_pan` | Cooking pan | 0–6 | — | 7 |
| `candlelight:stove_block` | All 11 stove variants | 1–3 | 4 | 0 |
| `farm_and_charm:roaster` | Roaster | 0–6 | — | 7 |
| `farm_and_charm:cooking_pot` | Cooking pot | 0–6 | — | 7 |
| `farm_and_charm:stove_block` | Stove | 1–3 | 4 | 0 |
| `farm_and_charm:silo` | Wooden and copper silos | 0–80² | — | 81–161² |
| `herbalbrews:tea_kettle` | Tea kettle, copper tea kettle | 1–6 | 7 | 0 |
| `herbalbrews:cauldron` | Potion mixing cauldron | 0–2, 4 | — | 3 |
| `meadow:cheese_form` | Cheese form | 1, 2 | — | 0 |
| `meadow:cooking_cauldron` | Cooking cauldron | 1–7 | — | 0 |
| `vinery:apple_press` | Apple press | 0, 2 | — | 1, 3³ |
| `vinery:fermentation_barrel` | Fermentation barrel | 0–4 | — | 5 |

¹ Brewery stores completed beer in a separate `beer` field, outside its three-slot
Container. Only ingredient insertion is available; beer collection and brewing
interactions remain manual. No synthetic output slot or reflective beer access
is added.

² A silo controller of capacity `N` exposes inputs `0..N-1` and outputs
`81..81+N-1`. Other slots and non-controller segments reject automation. Its
public `getCapacity()` method is the only reflective call, because `Container`
always reports 162 slots even for a single-block silo. Unsupported method versions
fail closed. Automation connects to the controller; it does not redirect to it.

³ Apple press slot 1 is the mashing recipe's output and the juice recipe's internal
input. Extraction there removes mash before the second stage; extract from slot
3 for the final product. Slot 2 accepts wine bottles. This follows the observed
`canPlaceItem` implementation, which rejects insertion into slot 1.

For pots/roaster/pan, slot 6 is the serving container input. Tea kettle slot 5
holds containers, slot 6 water and slot 7 heating items. Meadow cauldron slot 7
is the fluid-container input; fermentation barrel slot 0 is juice and slot 4
empty bottles. Empty buckets/bottles remaining in input or fuel slots must be
removed manually: the requested insertion-only rule also applies to remainders.

## Audit and exclusions

The block entity classes, recipe consumption/output methods, inventory
interfaces, menus, and registration code were inspected from these installed
Forge jars using CFR 0.152: API 1.2.15, Bakery 2.0.3, Beachparty 1.1.5, Brewery
2.0.3, Candlelight 2.0.2, Farm & Charm 1.0.4, Furniture 1.0.4, HerbalBrews 1.0.12,
Meadow 1.3.23, NetherVinery 1.2.19, Vinery 1.4.41, WilderNature 1.0.5.
These versions define the supported layouts; other versions are not certified.

The following processing blocks are deliberately **not overridden**:

| Registry ID | Observed input / fuel / output | Reason |
| --- | --- | --- |
| `farm_and_charm:mincer` | 0 / — / 1 | Extends `RandomizableContainerBlockEntity`; Forge already supplies `InvWrapper` |
| `farm_and_charm:crafting_bowl` | 0–3 / — / 4 | Same inherited Forge handler |
| `meadow:stove_block_entity` | 0 / 1 / 2 | Extends `SmokerBlockEntity`; existing furnace handlers |
| `herbalbrews:stove_block_entity` | 0 / 1 / 2 | Same existing furnace handlers |

Their native handlers retain their original behavior, including any less strict
input/output restrictions. This addon cannot impose its slot policy on them
without violating the requirement to preserve existing item handlers.

Other inspected blocks outside the adapter:

- HerbalBrews `tea_leaf`: timed drying changes the block state; the block entity
  has a timer, **no Container**, and no item slots. Not an inventory recipe machine.
- Meadow `cheese_rack`: holds two display stacks but **does not implement Container**;
  its block entity does not perform recipe processing.
- WilderNature `bounty_board`: contract/reward interactions, **no Container**;
  not an item-processing recipe machine.
- API, Furniture and NetherVinery have no additional recipe-processing block
  entities. Cabinets, drawers, baskets, racks, displays, mugs, jugs, flower pots,
  signs, banners and decorative entities are outside scope. Brewery components
  use block state and player interactions, not additional processing inventories.

All identified inventory recipe processors implement Container (directly or via
ImplementedInventory); there are no additional non-Container recipe processors
hidden behind a reflective adapter.

## Configuration

`config/letsdo_create_io-common.toml` is generated by Forge. Every supported block
entity type defaults to enabled. For example:

```toml
[blocks.farm_and_charm]
    roaster = false
    stove_block = true
```

Restart the server after changes so existing block entities are attached afresh.
A disabled entry disables all block variants sharing that block entity type.

## Build and verify

Use a **JDK 17**, including `javac` (a server JRE alone is insufficient).
Mappings are Mojang official 1.20.1. No Let's Do jars are required to compile the
production code. Local jars are runtime-only `fg.deobf` dependencies and are
excluded from Git and the distribution jar.

```sh
export JAVA_HOME=/path/to/jdk-17
./gradlew build
```

For integration tests, copy the above Let's Do jars plus Architectury
9.2.14 and Curios 5.14.1+1.20.1 (WilderNature dependency) into `libs/`, then run:

```sh
./gradlew runGameTestServer
```

The GameTest source set and empty test structure are separate from the release
jar. Tests place every registered block variant of each table entry, check all
faces, simulation, insertion/extraction direction, inactive silo slots and
capability invalidation. Brewery has no Container output, so only its input
contract can be tested. Missing table entries fail test registration rather than
silently passing. JUnit tests validate immutable slot roles and insertion limits;
GameTests also exercise the real ItemStack adapter, NBT, configuration gating,
and preservation of native/untargeted handlers.

Build output: `build/libs/letsdo_create_io-1.1.0.jar`.
JUnit results: `build/reports/tests/test/index.html`.
GameTest console: `run/logs/latest.log`.

## Verified result

On 2026-10-29, `./gradlew build` succeeded with JDK 17.0.21 and all five
JUnit tests passed. `./gradlew runGameTestServer` exited successfully with
**40/40 required tests passed**: 31 supported block variants across 17 entity
types (including per-face insertion limits), three face-routing checks (roaster
bowls and side extraction, stove fuel/ingredient split, Meadow cauldron water),
four native-handler checks, and two common-contract checks. All 12 requested
Let's Do jars loaded, together with Architectury and Curios; no mod was skipped.
Development runs remap dependency Mixin refmaps from SRG to the official mapping
using `createSrgToMcp`.

These tests establish inventory capability behavior, not complete automated
recipe production, Create transport timing, or manual-action replacement.

## License

MIT. Copyright (c) 2026 byongshintv. See [LICENSE](LICENSE).
Let's Do mods and their assets retain their respective licenses and are not
included in this repository or release.

This mod was implemented with AI assistance (OpenAI Codex) and reviewed by the maintainer.
