package folk.sisby.surveyor;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;

public class SurveyorConfig extends WrappedConfig {
    public enum SystemMode {
        DISABLED,
        FROZEN,
        DYNAMIC,
        ENABLED
    }

    public enum ShareMode {
        DISABLED,
        ENABLED,
        OMNISCIENT
    }

    @Comment("Zeroes out waypoint owner UUIDs to use the new 'host uuid' system, which mirrors vanilla behaviour")
    @Comment("Only occurs when loading into a singleplayer world - Will be removed in the next version")
    public boolean migrateSingleplayerLandmarksFrom05to06 = true;

    @Comment("Terrain system - records layers of blocks and biomes for maps to render")
    @Comment("DISABLED prevents loading, FROZEN loads but prevents updates, DYNAMIC loads with addons or on servers, ENABLED always loads.")
    public SystemMode terrain = SystemMode.DYNAMIC;

    @Comment("Structure system - records structure identifiers and piece data for specialized maps and utilities to render")
    @Comment("DISABLED prevents loading, FROZEN loads but prevents updates, DYNAMIC loads with addons or on servers, ENABLED always loads.")
    public SystemMode structures = SystemMode.DYNAMIC;

    @Comment("Landmark system - a generic record of both player-owned waypoints and server-owned POIs, accessible via API")
    @Comment("DISABLED prevents loading, FROZEN loads but prevents updates, DYNAMIC loads with addons or on servers, ENABLED always loads.")
    public SystemMode landmarks = SystemMode.DYNAMIC;

    @Comment("Whether to automatically add/remove nether portal landmarks")
    public boolean netherPortalLandmarks = true;

    @Comment("Whether to automatically add player death waypoints")
    public boolean playerDeathLandmarks = true;

    @Comment("Displays the following logs and messages:")
    @Comment("[Action Bar] Structure Discovery")
    public boolean debugMode = false;

    public Synchronization sync = new Synchronization();
    public static final class Synchronization implements Section {
        @Comment("[Server] Whether to place every player in a single share group")
        @Comment("Disables /surveyor share and /surveyor unshare")
        public boolean forceGlobal = false;

        @Comment("Sends clients group-explored terrain")
        @Comment("OMNISCIENT - all terrain is sent to all players")
        public ShareMode terrainSharing = ShareMode.ENABLED;

        @Comment("Sends clients group-explored structures")
        @Comment("OMNISCIENT - all structures are sent to all players")
        public ShareMode structureSharing = ShareMode.ENABLED;

        @Comment("Sends clients group-owned landmarks (waypoints)")
        @Comment("OMNISCIENT - all unowned landmarks are sent to all players")
        public ShareMode landmarkSharing = ShareMode.ENABLED;

        @Comment("[Server] Sends clients group-member position updates")
        @Comment("OMNISCIENT - all player positions are sent to all players")
        public ShareMode positionSharing = ShareMode.ENABLED;

        @Comment("[Server] Ticks per position update - lower is more frequent")
        @IntegerRange(min = 1, max = 200)
        public int positionTicks = 1;

        @Comment("Whether to sync missing data when joining a world / changing groups")
        @Comment("Must be enabled on both client and server")
        public boolean syncOnJoin = true;

        @Comment("[Client] Whether to skip sending waypoints to the server")
        public boolean privateWaypoints = false;
    }
}
