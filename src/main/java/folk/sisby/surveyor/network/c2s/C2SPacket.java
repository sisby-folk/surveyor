package folk.sisby.surveyor.network.c2s;

import folk.sisby.surveyor.network.SurveyorNetworking;
import folk.sisby.surveyor.network.SurveyorPacket;

public interface C2SPacket extends SurveyorPacket {
    default void send() {
        SurveyorNetworking.C2S_SENDER.accept(this);
    }
}
