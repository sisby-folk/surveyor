<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->
<center>
A wasteful but flexible library for client-side world maps (and serverside companion mod for those maps) <br/>
Used in <a href="https://modrinth.com/mod/antique-atlas-4">Antique Atlas 4</a>.<br/>
Requires <a href="https://modrinth.com/mod/connector">Connector</a> and <a href="https://modrinth.com/mod/forgified-fabric-api">FFAPI</a> on forge.<br/>
<i>Other names considered: Polaris, Ichnite, Trackway, Lay of the Land, Worldsense, and Lithography.</i>
</center>

---

Surveyor allows players to receive map data from the server for compatible world map mods.

This includes restoring lost exploration progress when swapping computers or deleting your instance.

### Developers

```groovy
repositories {
    maven { url 'https://repo.sleeping.town/' }
}

dependencies {
    modImplementation 'folk.sisby:surveyor:0.1.0-alpha.1+1.20'
    include 'folk.sisby:surveyor:0.1.0-alpha.1+1.20'
}
```

Surveyor is designed to remove the load of managing persistent storage from world map mods (client or web) that use it.

It does this by creating and saving abbreviated versions of chunks and structures called *summaries* - which are designed to contain the information a map mod would usually read directly from a chunk - such as the top non-air block, the heightmap, and the biome.

These summaries are quite large by map standards, but small enough that an entire dimension's worth can be feasibly loaded into memory or sent to a client at once.

### Afterword

All mods are built on the work of many others.

We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.
