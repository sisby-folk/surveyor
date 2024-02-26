package folk.sisby.surveyor.landmark;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.poi.PointOfInterestType;

public interface PointOfInterestLandmark<T extends PointOfInterestLandmark<T>> extends Landmark<T> {
    RegistryKey<PointOfInterestType> poiType();
}
