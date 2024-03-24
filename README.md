<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->


<center>
<img alt="surveyor banner" src="https://github.com/sisby-folk/surveyor/assets/55819817/a591c325-e87b-48cb-8e00-bda80c9ac8a2"><br/>
A unified save and server-integration framework for client-side map mods (and others).<br/>
Used in <a href="https://modrinth.com/mod/antique-atlas-4">Antique Atlas 4</a>. Try it with <a href="https://github.com/HestiMae/surveyor-surveyor">SurveyorSurveyor</a>!<br/>
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
    modImplementation 'folk.sisby:surveyor:0.1.0-beta.8+1.20'
    include 'folk.sisby:surveyor:0.1.0-beta.8+1.20'
}
```

### Core Concepts

The **World Summary** holds all of surveyor's data for a world. It can be accessed using `WorldSummary.of(World)`.

**Chunk Summaries** (or the "Terrain Summary") represent the world viewed from above. This includes the top layer of blocks, along with their biome, height, block light level, and the depth of water above them.

**Structure Summaries** represent an in-world structure (called `StructureStart` in yarn) - they include map-critical information for identifying the structure and its pieces, but not any actual blocks or piece NBT.

**Landmarks** are a way to represent all other positional information on-map. They have unique serialization per-type, and are uniquely keyed by their type and position to prevent overlaps.


### Terrain Summary Layers

In order to facilitate cave mapping, Surveyor records the top layer of blocks at **multiple height levels** (layer heights).

**The Overworld** scans for floors in these layers:
- 257-319 - usually empty
- 62-256 - surface terrain
- 0-61 - sea floors and riverbeds, ravines, and caves
- -64-0 - deepslate caves

**The Nether** scans for floors in these layers:
* 127-255 - usually flat bedrock
* 71-126 - high caves and outcrops
* 41-70 - mid-level outcrops and walkways
* 0-40 - the lava sea and shores

Roughly speaking, Surveyor will accept any non-clear block within or below a 2-high walk-space as a floor.

Surveyor supports any layer height configuration, but currently lacks the API/config to change this for specific dimensions.

Note that the amount of layers doesn't affect how mods display the map, only how often cave floors will be occluded by floors above them.

### Map Mods

<details>
<summary>Click to show the map mod guide</summary>

Quick reminder that surveyor should **replace any existing world scanning logic**<br/>
You should never need to look at the currently loaded chunks - If some information is missing, let us know!

#### Initial Setup

Tune into `SurveyorClientEvents.ClientPlayerLoad` - this will trigger when the client world has access to surveyor data and the player is available.

`WorldTerrainSummary.keySet()` contains all available chunks by position. You can also use `bitSet()` and `toKeys()` if you want to sort the keys by region.

`WorldStructureSummary.keySet()` contains all structure starts by key + ChunkPos.

`WorldLandmarks.keySet()` contains all landmarks (POIs, waypoints, death markers, etc.) by type + BlockPos.

To use these on the client, pass in `SurveyorClient.getExploration(ClientPlayer)`.<br/>
This ensures surveyor will hide any areas the current player hasn't explored, or waypoints they didn't make.

##### Live Updates

You should also tune into the `TerrainUpdated`, `StructuresAdded`, `LandmarksAdded`, and `LandmarksRemoved` events, which will fire whenever the world summary changes.

Note that these events might fire before `ClientPlayerLoad`, so skip them if you haven't initialized your map data there yet!

You don't need to check exploration when listening to these methods on the client - their contents are already explored.

#### Terrain Rendering

First, generate a top layer (with any desired height limits) using `WorldTerrainSummary.get(ChunkPos).toSingleLayer()`.<br/>
This will produce a raw layer summary of one-dimensional arrays:
* **exists** - True where a floor exists, false otherwise - where false, all other fields are junk.
* **depths** - The distance of the floor below your specified world height. so y = worldHeight - depth.
* **blocks** - The floor block. Indexed per-region via `WorldTerrainSummary.getBlockPalette(ChunkPos)`.
* **biomes** - The floor biome. Indexed per-region via `WorldTerrainSummary.getBiomePalette(ChunkPos)`.
* **lightLevels** - The block light level directly above the floor (i.e. the block light for its top face). 0-15.
* **waterDepths** - How deep the contiguous water above the floor is.
  * All other liquid surfaces are considered floors, but water is special-cased.
  * The sea floor (e.g. sand) is recorded, and this depth value indicates the water surface instead.
  * This allows maps to show water depth shading, but also hide water completely if desired.

All arrays can be indexed by `x * 16 + z`, where x and z are relative to the chunk.<br/>
Use these arrays to render and store map data for that chunk (pixels, buffers, whichever).<br/>
Remember that you'll be rendering hundreds of thousands of chunks here - optimize this process hard.

#### Structure Rendering

Along with the key and ChunkPos, you can get the type and any tags using `WorldStructureSummary.getType(key)` and `WorldStructureSummary.getTags(key)`.

You can access a full summary of the structure (e.g. to draw its bounding boxes) using `WorldStructureSummary.get(key, ChunkPos)`.<br/>
This includes piece data like boxes, direction, IDs, etc.

#### Landmark Rendering & Management

Along with the type and BlockPos, you can get a full landmark using `WorldLandmarks.get(type, BlockPos)`.

By default, this can include a dye color, a text name, the owner's UUID, and a texture (could be from another map mod).<br/>
You should have a method of rendering a landmark using just this information.

To improve how landmarks are displayed, you can use `instanceof` to check for additional data, e.g. `HasBlockBox`.

To add a custom waypoint landmark, just construct a `SimplePointLandmark` owned by the client player, and add it using `WorldLandmarks.put(Landmark)`. This will save to disk and send a copy to the server.

</details>

### Landmark Integrations

<details>
<summary>Click to show the landmark integration guide</summary>

Landmark types can be registered via the registry in `Landmarks`.<br/>
This allows you to set and serialize custom data relevant to your landmark.<br/>
Your landmark can usually be a record. Check the [builtins](https://github.com/sisby-folk/surveyor/tree/1.20/src/main/java/folk/sisby/surveyor/landmark) for an example.

To make extra landmark data accessible to map mods, always declare a new `Has` interface to access it from.

To place a landmark, just use `WorldLandmarks.put(Landmark)`.<br/>
This works fine on either side - adding a landmark on the server will send it to the client and vice-versa.

Landmark types can't yet have fallback types - so use a simple type (or PR a new one!) if your mod is only on one side.

</details>

## Afterword

All mods are built on the work of many others.

**Special thanks to:**<br/>
[Ampflower](https://github.com/Ampflower), [Falkreon](https://github.com/falkreon), [Garden](https://modrinth.com/user/GardenSystem), [Kat](https://git.sleeping.town/Kat), [Solo](https://github.com/solonovamax), [Crosby](https://github.com/RacoonDog), [Lemma](https://github.com/LemmaEOF), [Leo](https://github.com/leo60228), [Jasmine](https://github.com/jaskarth), [Aqua](https://github.com/Aquaeyes), [Wonder](https://git.sleeping.town/wonder), [Infinidoge](https://github.com/Infinidoge), [Emi](https://github.com/emilyploszaj), and [Una](https://github.com/unascribed).

We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.
