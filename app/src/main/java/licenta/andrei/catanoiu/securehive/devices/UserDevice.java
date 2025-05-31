package licenta.andrei.catanoiu.securehive.devices;

import com.google.firebase.Timestamp;

public class UserDevice {
    private String deviceId;
    private String customName;
    private Timestamp addedAt;

    public UserDevice() {
        // Constructor gol necesar pentru Firebase
    }

    public UserDevice(String deviceId, String customName) {
        this.deviceId = deviceId;
        this.customName = customName != null && !customName.trim().isEmpty() ? customName.trim() : deviceId;
        this.addedAt = Timestamp.now();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName != null && !customName.trim().isEmpty() ? customName.trim() : this.deviceId;
    }

    public Timestamp getAddedAt() {
        return addedAt;
    }

    @Override
    public String toString() {
        return customName;
    }
} 