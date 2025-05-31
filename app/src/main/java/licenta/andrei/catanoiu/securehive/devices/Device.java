package licenta.andrei.catanoiu.securehive.devices;

import com.google.firebase.Timestamp;

public class Device {
    public enum DeviceStatus {
        ONLINE,
        OFFLINE,
        MAINTENANCE
    }

    private String id;
    private DeviceStatus status;
    private Timestamp lastUpdated;

    public Device() {
        // Constructor gol necesar pentru Firebase
    }

    public Device(String id) {
        this.id = id;
        this.status = DeviceStatus.OFFLINE;
        this.lastUpdated = Timestamp.now();
    }

    public String getId() {
        return id;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
        this.lastUpdated = Timestamp.now();
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public boolean isActive() {
        return status == DeviceStatus.ONLINE;
    }
}