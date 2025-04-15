package licenta.andrei.catanoiu.securehive;

public class Device {
    private String id;
    private String name;
    private String ipAddress;
    private boolean active;

    public Device(String id, String name, String ipAddress, boolean active) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return name;
    }
}