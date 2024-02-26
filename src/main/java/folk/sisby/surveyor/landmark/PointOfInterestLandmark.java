package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Encoder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.poi.PointOfInterestType;

public interface PointOfInterestLandmark<T extends PointOfInterestLandmark<T>> extends Landmark<T> {
    RegistryKey<PointOfInterestType> poiType();

    default Encoder<? extends PointOfInterestLandmark<?>> fallback() {
        return SimplePointOfInterestLandmark.FallbackEncoder.INSTANCE;
    }
}
