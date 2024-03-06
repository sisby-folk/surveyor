package folk.sisby.surveyor.landmark;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.poi.PointOfInterestType;

public interface HasPoiType {
    RegistryKey<PointOfInterestType> poiType();
}
