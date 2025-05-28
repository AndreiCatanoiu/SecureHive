package licenta.andrei.catanoiu.securehive.devices;

public class Device {
    public enum DeviceStatus {
        ONLINE,
        OFFLINE,
        MAINTENANCE
    }

    private String id;
    private String name;
    private String lastAlert;
    private DeviceStatus status;

    public Device(String id, String name, String lastAlert, DeviceStatus status) {
        this.id = id;
        this.name = name;
        this.lastAlert = lastAlert;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastAlert() {
        return lastAlert;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == DeviceStatus.ONLINE;
    }

    @Override
    public String toString() {
        return name;
    }
}