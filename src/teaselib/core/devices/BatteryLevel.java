package teaselib.core.devices;

public enum BatteryLevel {
    Empty,
    Low,
    Medium,
    High;

    public boolean isSuffcient() {
        return this == BatteryLevel.Medium || this == High;
    }

    public boolean needsCharging() {
        return this == Empty || this == Low;
    }
}
