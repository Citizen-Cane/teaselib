package teaselib.core.devices;

public abstract class WiredDevice implements Device {

    @Override
    public BatteryLevel batteryLevel() {
        return BatteryLevel.High;
    }

    @Override
    public boolean isWireless() {
        return false;
    }

}
