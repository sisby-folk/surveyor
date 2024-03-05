package folk.sisby.surveyor;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;

public class SurveyorConfig extends WrappedConfig {
    @Comment("Various debug loggers and messages")
    public final Boolean debugMode = false;
}
