package folk.sisby.surveyor.packet.c2s;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.Landmarks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Map;

public record OnLandmarkAddedC2SPacket(Landmark<?> landmark) implements C2SPacket {
    public OnLandmarkAddedC2SPacket(PacketByteBuf buf) {
        this(
            Landmarks.CODEC.decode(NbtOps.INSTANCE, buf.readNbt()).getOrThrow(false, Surveyor.LOGGER::error).getFirst().values().stream().findFirst().orElseThrow().values().stream().findFirst().orElseThrow()
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeNbt((NbtCompound) Landmarks.CODEC.encodeStart(NbtOps.INSTANCE, Map.of(landmark.type(), Map.of(landmark.pos(), landmark))).getOrThrow(false, Surveyor.LOGGER::error));
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.C2S_ON_LANDMARK_ADDED;
    }
}
