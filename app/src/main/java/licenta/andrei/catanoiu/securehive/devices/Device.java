package licenta.andrei.catanoiu.securehive.devices;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;

public class Device implements Parcelable {
    public enum DeviceStatus {
        ONLINE,
        OFFLINE,
        MAINTENANCE
    }

    private String id;
    private String name;
    private DeviceStatus status;
    private String imageUrl;
    private String lastAlert;
    private String deviceType;
    private long lastSeen;
    private Timestamp lastUpdated;

    public Device() {
    }

    public Device(String id, String name, DeviceStatus status, String imageUrl) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.imageUrl = imageUrl;
        this.lastSeen = System.currentTimeMillis();
        this.lastUpdated = Timestamp.now();
    }

    public Device(String id, String name, DeviceStatus status, String imageUrl, String deviceType) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.imageUrl = imageUrl;
        this.deviceType = deviceType;
        this.lastSeen = System.currentTimeMillis();
        this.lastUpdated = Timestamp.now();
    }

    protected Device(Parcel in) {
        id = in.readString();
        name = in.readString();
        status = DeviceStatus.valueOf(in.readString());
        imageUrl = in.readString();
        lastAlert = in.readString();
        deviceType = in.readString();
        lastSeen = in.readLong();
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
        if (status == DeviceStatus.ONLINE) {
            this.lastSeen = System.currentTimeMillis();
        }
        this.lastUpdated = Timestamp.now();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLastAlert() {
        return lastAlert;
    }

    public void setLastAlert(String lastAlert) {
        this.lastAlert = lastAlert;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isOnline() {
        return status == DeviceStatus.ONLINE;
    }

    public boolean isOffline() {
        return status == DeviceStatus.OFFLINE;
    }

    public boolean isMaintenance() {
        return status == DeviceStatus.MAINTENANCE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(status != null ? status.name() : null);
        dest.writeString(imageUrl);
        dest.writeString(lastAlert);
        dest.writeString(deviceType);
        dest.writeLong(lastSeen);
    }
}