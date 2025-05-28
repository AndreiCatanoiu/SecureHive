package licenta.andrei.catanoiu.securehive.models;

public class ActivityItem {
    private String deviceName;
    private String description;
    private String timestamp;

    public ActivityItem(String deviceName, String description, String timestamp) {
        this.deviceName = deviceName;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
} 