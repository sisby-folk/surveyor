package folk.sisby.surveyor;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;

public class SurveyorConfig extends WrappedConfig {
    @Comment("Various debug loggers and messages")
    public Boolean debugMode = false;

    @Comment("Whether to share all terrain exploration all the time")
    public Boolean shareAllTerrain = false;

    @Comment("Whether to share all structure exploration all the time")
    public Boolean shareAllStructures = false;

    @Comment("Whether to share all landmarks all the time")
    public Boolean shareAllLandmarks = false;

    @Comment("How many ticks to wait before sending the new location of player 'friends'")
    @IntegerRange(min = 1, max = 200)
    public Integer ticksPerFriendUpdate = 2;
}
