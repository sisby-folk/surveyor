package folk.sisby.surveyor.landmark;

public interface EventLandmark<T extends EventLandmark<T>> extends Landmark<T> {
    long created();
}
