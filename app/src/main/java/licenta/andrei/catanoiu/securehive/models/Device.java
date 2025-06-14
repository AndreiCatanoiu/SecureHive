package licenta.andrei.catanoiu.securehive.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Device implements Parcelable {
    private String id;
    private String name;
    private String status;
    private String imageUrl;
    private String lastAlert;
    private String deviceType;
    private long lastSeen;

    public Device() {
    }

    public Device(String id, String name, String status, String imageUrl) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.imageUrl = imageUrl;
        this.lastSeen = System.currentTimeMillis();
    }

    public Device(String id, String name, String status, String imageUrl, String deviceType) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.imageUrl = imageUrl;
        this.deviceType = deviceType;
        this.lastSeen = System.currentTimeMillis();
    }

    protected Device(Parcel in) {
        id = in.readString();
        name = in.readString();
        status = in.readString();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("online".equalsIgnoreCase(status)) {
            this.lastSeen = System.currentTimeMillis();
        }
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

    public boolean isOnline() {
        return "online".equalsIgnoreCase(status);
    }

    public boolean isOffline() {
        return "offline".equalsIgnoreCase(status);
    }

    public boolean isInactive() {
        return "inactive".equalsIgnoreCase(status);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(status);
        dest.writeString(imageUrl);
        dest.writeString(lastAlert);
        dest.writeString(deviceType);
        dest.writeLong(lastSeen);
    }
} 