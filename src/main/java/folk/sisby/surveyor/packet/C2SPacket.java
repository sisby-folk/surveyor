package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;

public interface C2SPacket extends SurveyorPacket {
	default void send() {
		SurveyorNetworking.C2S_SENDER.accept(this);
	}
}
