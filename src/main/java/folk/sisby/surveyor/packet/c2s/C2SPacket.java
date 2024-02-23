package folk.sisby.surveyor.packet.c2s;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.packet.SurveyorPacket;

public interface C2SPacket extends SurveyorPacket {
    default void send() {
        SurveyorNetworking.C2S_SENDER.accept(this);
    }
}
