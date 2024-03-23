<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->


<center>
<img alt="surveyor banner" src="https://github.com/sisby-folk/surveyor/assets/55819817/a591c325-e87b-48cb-8e00-bda80c9ac8a2"><br/>
A unified save and server-integration framework for client-side map mods (and others).<br/>
Used in <a href="https://modrinth.com/mod/antique-atlas-4">Antique Atlas 4</a>.<br/>
<!-- Requires <a href="https://modrinth.com/mod/connector">Connector</a> and <a href="https://modrinth.com/mod/forgified-fabric-api">FFAPI</a> on forge.<br/> -->
<i>Other names considered: Polaris, Ichnite, Trackway, Lay of the Land, Worldsense, and Lithography.</i>
</center>

---

> *Surveyor is a library for map mod developers! You shouldn't need to download it alone.*

**Surveyor** is a map library that:
* Records terrain, structure, and "landmark" data suitable for maps as the world is explored / changed.
* Holds the data in a small, compressed format in-memory and on-disk to allow full dimensions to be loaded.
* Uses data formats that are map-mod-agnostic - i.e:
  * Terrain data is recorded as "floors" for each x,z - including the height, block, biome, and light level.
  * Terrain data records multiple layers of floors, allowing for usable cave and nether maps.
  * Structures are recorded with all their base data (pieces, jigsaws with IDs etc.) intact.
  * Landmarks can generically represent all other positional map data - e.g. waypoints, POIs, or faction claims.
* Syncs structure summaries to the client for use on maps.
* Restores missing terrain and landmark data from the server if the client loses it.
* Removes the need for map mods to implement save data or networking in most cases.


### Configuration

To force surveyor to show and share as much map data as possible globally, you can set the `shareAll` settings in `config/surveyor.toml`.

---

**Notice: Surveyor is still early in development.**
- The API might break several times during 0.x
- The networking format will break several times during 0.x.
- The save format will likely break on the change to 1.x
- Javadoc is very limited

---

## Developers

```groovy
repositories {
    maven { url 'https://repo.sleeping.town/' }
}

dependencies {
    modImplementation 'folk.sisby:surveyor:0.1.0-beta.1+1.20'
    include 'folk.sisby:surveyor:0.1.0-beta.1+1.20'
}
```

### Core Concept - Terrain Summaries

**Terrain Summaries** (or "Chunk" or "Region" summaries) represent blocks for rendering on maps.<br/>
They're added and changed per-chunk, but paletted and saved per-region.<br/>
Each chunk summary consists of **Floors**, non-clear solid blocks below two contiguous non-solid blocks.<br/>
Floors are stored in **Layers**, which hold the topmost floors for each x,z column over specific range of y-values.<br/>
This is best explained by example.

**The Overworld** has layers at:
- 319 - always empty unless players build there
- 256 - most surface terrain
- 61 - the sea floor, ravines, and caves
- 0 - deepslate caves

**The Nether** has layers at:
* 255 - empty bedrock ceiling unless players use exploits to build there
* 126 - high caves and outcrops
* 70 - mid-level outcrops and walkways
* 40 - the lava sea and shores

The layering dictates the **maximum number of floors surveyor can detect in an x,z column**.<br/>
If it finds one floor in a layer, it can't find another floor until the next layer starts.<br/>
More layers is "higher definition" - harder to miss a cave underneath another cave - but will take up more space.

Layering does not dictate **how maps display floors** - surveyor can provide the top known floors between any two arbitrary y values, so cave maps can easily be overlayed, combined, displayed as transparent, etc.

### Other Concepts

The **World Summary** holds all of surveyor's data for a world. It can be accessed through the `SurveyorWorld` duck.

**Structure Summaries** represent an in-world structure (called `StructureStart` in yarn) - they include map-critical information for identifying the structure and its pieces, but not any actual blocks or piece NBT. 

**Landmarks** are a way to represent all other positional information on-map. They have unique serialization per-type, and are uniquely keyed by their type and position to prevent overlaps.

### Map Mods

<details>
<summary>Click to show the map mod guide</summary>

Quick reminder that surveyor should **replace your existing world scanning logic**<br/>
You should never need to look at the currently loaded chunks - Otherwise, let us know and we'll try extend surveyor!

#### Initial Setup

Tune into loading via `SurveyorClientEvents.Register.clientPlayerLoad` - this will trigger when the client world has access to surveyor data and the player is available (for prioritizing nearby regions).

You can call `WorldSummary.terrain().keySet()` to get all summarized chunk positions - feel free to add these to a queue or deque to render later.

`keySet()` and `asMap()` methods accept `SurveyorExploration` - on the client this can be accessed from `SurveyorClient.getExploration()` - which ensures only map area the player should see is shown.

#### Terrain Rendering

To process a chunk, first get the summary using `WorldTerrainSummary.get(ChunkPos)`.<br/>
Remember you can always get the world summary from using `SurveyorWorld` if you're processing on world tick.<br/>
Then, crunch the result into floors using `ChunkSummary.toSingleLayer()` which outputs usable int arrays:
* **exists[256]** - Set where a floor exists, unset otherwise - where unset, all other fields are junk.
* **depths[256]** - The distance of the floor below your specified world height. so y = worldHeight - depth.
* **blocks[256]** - The floor block. Indexed per-region via `WorldTerrainSummary.getBlockPalette(ChunkPos)`.
* **biomes[256]** - The floor biome. Indexed per-region via `WorldTerrainSummary.getBiomePalette(ChunkPos)`.
* **lightLevels[256]** - The block light level directly above the floor (i.e. the block light for its top face). 0-15.
* **waterDepths[256]** - How deep the contiguous water above the floor is.
  * All other liquid surfaces are considered floors, but water is special-cased.
  * The sea floor (e.g. sand) is recorded, and this depth value indicates the water surface instead.
  * This allows maps to show water depth shading, but also hide water completely if desired.

For all these arrays, the index is (x * 16 + z), where x and z are relative to the chunk.

Using this data, render usable data for your map (color buffers, images, etc) and hold onto them per-world.<br/>
You may be rendering hundreds of thousands of chunks here - this is the hot loop, that's why it's all ugly int arrays.

#### Structure Rendering

Structures can be retrieved using `WorldStructureSummary.asMap()` - these come in a `StructureSummary` format, which clearly defines all type-independent data, along with extra data for jigsaws.

These can be used to create automatic waypoints for structures, draw abstract versions of them to the map by ID, etc.

#### Landmark Rendering & Management

Landmarks can be retrieved using `WorldLandmarks.asMap()` - or one of the type or class specific getters as needed.

Landmarks can be most simply represented as a point on the map. They may include a dye color (for vanilla banner style rendering) as well as some name text for labels or tooltips.

Landmarks can also include a texture identifier, which may or may not exist on your client, depending on how it was made.

To add a custom waypoint landmark, just construct a `SimplePointLandmark` owned by the client player, and add it using `WorldLandmarks.put(Landmark)`. This will save to disk and send a copy to the server.

#### Live Updates

You should also tune into the `TerrainUpdated`, `StructuresAdded`, `LandmarksAdded`, and `LandmarksRemoved` events, which will fire whenever the world summary changes.

#### Examples

An implementation of a surveyor map mod (with advanced terrain rendering) can be found in [Antique Atlas](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/java/folk/sisby/antique_atlas/WorldTiles.java).

A minecraftless vanilla-map-like implementation that reads surveyor's NBT save files directly is [SurveyorSurveyor](https://github.com/HestiMae/surveyor-surveyor/blob/main/src/main/java/garden/hestia/surveyor_surveyor/SurveyorSurveyor.java).

</details>

### Landmark Integrations

<details>
<summary>Click to show the landmark integration guide</summary>

Landmark types can be registered via the registry in `Landmarks`.<br/>
This allows you to set and serialize custom data relevant to your landmark.<br/>
Your landmark can usually be a record.

To make new data accessible to map mods, declare a new interface to access it from, so it can be applied to more than one type.

To add a landmark (custom or builtin), just use `WorldLandmarks.put(Landmark)`. This works fine on either side - feel free to add a landmark on the server to send it to the client.

Landmark types can't yet have fallback types - so use a simple type (or PR a new one!) if your mod is only on one side.

</details>

### Dimension Mods

<details>
<summary>Click to show the dimension mod integration guide</summary>

Chunk summaries are currently layered based on the dimension via a few basic heuristics on world height, ceiling and sky presence, etc. - as well as a few hardcoded layer additions for the nether.

In future, we'll expose an API to allow dimension mods to specify the layers to generate chunk summaries with.

</details>

## Afterword

All mods are built on the work of many others.

**Special thanks to:**<br/>
[Ampflower](https://github.com/Ampflower), [Falkreon](https://github.com/falkreon), [Garden](https://modrinth.com/user/GardenSystem), [Kat](https://git.sleeping.town/Kat), [Solo](https://github.com/solonovamax), [Crosby](https://github.com/RacoonDog), [Lemma](https://github.com/LemmaEOF), [Leo](https://github.com/leo60228), [Jasmine](https://github.com/jaskarth), [Aqua](https://github.com/Aquaeyes), [Wonder](https://git.sleeping.town/wonder), [Infinidoge](https://github.com/Infinidoge), [Emi](https://github.com/emilyploszaj), and [Una](https://github.com/unascribed).

We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.
