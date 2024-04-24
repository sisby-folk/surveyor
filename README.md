<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->


<center>
<img alt="surveyor banner" src="https://cdn.modrinth.com/data/4KjqhPc9/images/f84b10d3e0257c66e4f60066f3f570bc26ca34b2.png"><br/>
Unified API, networking, and save data for map mods.<br/>
Used in <a href="https://modrinth.com/mod/antique-atlas-4">Antique Atlas 4</a>.
Requires <a href="https://modrinth.com/mod/connector">Connector</a> and <a href="https://modrinth.com/mod/forgified-fabric-api">FFAPI</a> on forge.<br/>
<i>Other names considered: Polaris, Ichnite, Trackway, Lay of the Land, Worldsense, and Lithography.</i>
</center>

---

### Player Usage

> *Surveyor is a library for map mod developers! You shouldn't need to download it alone.*

#### Commands

* `/surveyor` - display information about your map exploration, including sharing.
* `/surveyor share [username]` - request/accept sharing map exploration with a player.
* `/surveyor unshare` - stop sharing map exploration (leave your "sharing group")

#### Configuration

* `ticksPerFriendUpdate` - how often to sync the position of sharing players to eachother (server)
* `shareAll{type}` - whether to ignore exploration and show all available data when possible.

---

### Library Features

**Surveyor relieves the scanning, saving, and networking responsibilities from dependent map mods.**

In general, Surveyor:
* Records terrain, structure, and "landmark" data suitable for maps as the world is changed.
* Enables live map sharing between players by tracking individual exploration of the map.
* Sends the client structures as they're discovered or shared.
* Syncs map data and landmarks (e.g. waypoints) when sharing or on client data loss.
* Exposes a generic API for map mod integrations (e.g. adding map markers to important locations).
* Only adds 2-5% to save size, using an efficient format both in-memory and on-disk.

Surveyor's data **deliberately preserves key details**, designed to allow any abitrary map mod to use it:
* Terrain is a top-down view of blocks with height, biome, light level, and water depth.
* Terrain contains multiple layers, allowing for usable cave and nether maps.
* Structures have IDs, pieces, tags, and even full piece NBT for smaller structures intact.
* Landmarks generically represent other positional map data - e.g. waypoints, POIs, or faction claims.

---

**Notice: Surveyor is still in early releases.**
* The API might break several times during 0.x
* The networking format will break several times during 0.x.
* The save format will likely break on the change to 1.x
* Javadoc is very limited

---

## Developers

```groovy
repositories {
    maven { url 'https://repo.sleeping.town/' }
}

dependencies {
    modImplementation 'folk.sisby:surveyor:0.3.0+1.20'
    include 'folk.sisby:surveyor:0.3.0+1.20'
}
```

#### Examples

* **[Antique Atlas 4](https://github.com/sisby-folk/antique-atlas)** - A stylized client-side map mod.
* **[SurveyorSurveyor](https://github.com/HestiMae/surveyor-surveyor)** - An enhanced-vanilla style java map image generator, using raw surveyor save files.
* **[Surveystones](https://github.com/sisby-folk/antique-fwaystones)** - A mixed-side addon that automatically adds landmarks for waystones from [Fabric Waystones](https://modrinth.com/mod/fwaystones/versions).

### Core Concepts

The **World Summary** holds all of surveyor's data for a world. It can be accessed using `WorldSummary.of(World)`.

**Chunk Summaries** (or the "Terrain Summary") represent the world viewed from above. This includes the top layer of blocks, along with their biome, height, block light level, and the depth of water above them.

**Structure Summaries** represent an in-world structure (called `StructureStart` in yarn) - they include map-critical information for identifying the structure and its pieces, but not any actual blocks or piece NBT.

**Landmarks** are a way to represent all other positional information on-map. They have unique serialization per-type, and are uniquely keyed by their type and position to prevent overlaps.

**Exploration** is a record of what chunks, structures, and landmarks a player should be able to see.<br/>
A player explores a chunk when they're sent it, explores a structure when they stand in (or look at) one of its pieces, and explores an (unowned) landmark when they've explore the chunk it's in. 

### Terrain Summary Layers

To facilitate cave mapping, Surveyor records the top layer of blocks at **multiple height levels** (layer heights).

**The Overworld** scans for floors in these layers:
* 257-319 - usually empty
* 62-256 - surface terrain
* 0-61 - sea floors and riverbeds, ravines, and caves
* -64-0 - deepslate caves

**The Nether** scans for floors in these layers:
* 127-255 - usually flat bedrock
* 71-126 - high caves and outcrops
* 41-70 - mid-level outcrops and walkways
* 0-40 - the lava sea and shores

Roughly speaking, Surveyor will accept any non-clear solid block below a 2-high walk-space as a floor.<br/>
It will also detect "carpets" (non-clear non-solid blocks) above these floors and use those instead.

Surveyor supports any layer height configuration, but currently lacks the API/config to change this for specific dimensions.

Note that the amount of layers doesn't affect how mods display the map, only how often cave floors will be occluded by floors above them.

### Map Mods

<details>
<summary>Click to show the map mod guide</summary>

Quick reminder that surveyor should **replace any existing world scanning logic**<br/>
You should never need to look at the currently loaded chunks - If some information is missing, let us know!

#### Initial Setup

Client map mods should always use `SurveyorClientEvents` - this ensures only explored areas will be provided in singleplayer.

Tune into `WorldLoad` and queue up the provided keys for rendering.<br/>
This event will trigger when the client world has access to surveyor data and the player is available.

`terrain` contains all available chunks by region. `WorldTerrainSummary.toKeys()` converts this into ChunkPos.<br/>
`structures` contains all structure starts by key + ChunkPos.<br/>
`landmarks` contains all landmarks (POIs, waypoints, death markers, etc.) by type + BlockPos.

You can get these from the world summary later using `keySet()` methods - check the event implementation.<br/>
Pass in `SurveyorClient.getExploration()` to ensure unexplored areas are hidden.

##### Live Updates

Also tune into `TerrainUpdated`, `StructuresAdded`, `LandmarksAdded` to add to your render queues.<br/>
These fire whenever the client player should see something new (usually via exploration).<br/>
They can also fire before `ClientPlayerLoad`, so let any of them create your map data.

Tune into `LandmarksRemoved` as well but without a queue - just remove from your map/queue directly.

#### Terrain Rendering

First, generate a top layer (with any desired height limits) using `get(ChunkPos).toSingleLayer()`.<br/>
This will produce a raw layer summary of one-dimensional arrays:
* **exists** - True where a floor exists, false otherwise - where false, all other fields are junk.
* **depths** - The distance of the floor below your specified world height. so y = worldHeight - depth.
* **blocks** - The floor block. Indexed per-region via `getBlockPalette(ChunkPos)`.
* **biomes** - The floor biome. Indexed per-region via `getBiomePalette(ChunkPos)`.
* **lightLevels** - The block light level directly above the floor (i.e. the block light for its top face). 0-15.
* **waterDepths** - How deep the contiguous water above the floor is.
  * All other liquid surfaces are considered floors, but water is special-cased.
  * The sea floor (e.g. sand) is recorded, and this depth value indicates the water surface instead.
  * This allows maps to show water depth shading, but also hide water completely if desired.

All arrays can be indexed by `x * 16 + z`, where x and z are relative to the chunk.<br/>
Use these arrays to render and store map data for that chunk (pixels, buffers, whichever).<br/>
Remember that you'll be rendering hundreds of thousands of chunks here - optimize this process hard.

#### Structure Rendering

Along with the key and ChunkPos, you can get the type and any tags using `getType(key)` and `getTags(key)`.

You can access a full summary of the structure (e.g. to draw its bounding boxes) using `get(key, ChunkPos)`.<br/>
This includes piece data like boxes, direction, IDs, etc.

#### Landmark Rendering & Management

Along with the type and BlockPos, you can get a full landmark using `get(type, BlockPos)`.

By default, this can include a dye color, a text name, the owner's UUID, and a texture (could be from another map mod).<br/>
You should have a method of rendering a landmark using just this information.

To improve how landmarks are displayed, you can use `instanceof` to check for additional data, e.g. `HasBlockBox`.

To add a waypoint landmark, just make a `SimplePointLandmark` owned by the player and use `put(Landmark)`.<br/>
This will save to disk and send a copy to the server.

#### Player Rendering

You can use `SurveyorClient.getFriends()` to get a set of players to draw on the map.

This includes both the client player, online "friends" (map sharing group members), and offline friends.

The players are represented abstractly, providing UUID, username, global position, yaw, and online status.

</details>

### Landmark Integrations

<details>
<summary>Click to show the landmark integration guide</summary>

Landmark types can be registered via the registry in `Landmarks`.<br/>
This allows you to set and serialize custom data relevant to your landmark.<br/>
Your landmark can usually be a record. Check the [builtins](https://github.com/sisby-folk/surveyor/tree/1.20/src/main/java/folk/sisby/surveyor/landmark) for an example.

To make extra landmark data accessible to map mods, always declare a new `Has` interface to access it from.

To place a landmark, just use `WorldSummary.of(world).landmarks().put(Landmark)`.<br/>
This works fine on either side - adding a landmark on the server will send it to the client and vice-versa.

Landmark types can't yet have fallback types - so use a simple type (or PR a new one!) if your mod is only on one side.

</details>

## Afterword

All mods are built on the work of many others.

**Special thanks to:**<br/>
[Ampflower](https://github.com/Ampflower), [Falkreon](https://github.com/falkreon), [Garden](https://modrinth.com/user/GardenSystem), [Kat](https://git.sleeping.town/Kat), [Solo](https://github.com/solonovamax), [Crosby](https://github.com/RacoonDog), [Lemma](https://github.com/LemmaEOF), [Leo](https://github.com/leo60228), [Jasmine](https://github.com/jaskarth), [Aqua](https://github.com/Aquaeyes), [Wonder](https://git.sleeping.town/wonder), [Infinidoge](https://github.com/Infinidoge), [Emi](https://github.com/emilyploszaj), and [Una](https://github.com/unascribed).

We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.
