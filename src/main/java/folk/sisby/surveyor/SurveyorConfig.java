package folk.sisby.surveyor;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;

public class SurveyorConfig extends WrappedConfig {
    @Comment("Various debug loggers and messages")
    public final Boolean debugMode = false;

    @Comment("Whether to share all terrain exploration all the time")
    public final Boolean shareAllTerrain = false;

    @Comment("Whether to share all structure exploration all the time")
    public final Boolean shareAllStructures = false;

    @Comment("Whether to share all landmarks all the time")
    public final Boolean shareAllLandmarks = false;
}
