package folk.sisby.surveyor.landmark;

public interface FlavourLandmark<T extends FlavourLandmark<T>> extends Landmark<T> {
    int seed();
}
