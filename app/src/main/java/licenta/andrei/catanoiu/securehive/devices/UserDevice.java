package licenta.andrei.catanoiu.securehive.devices;

import android.os.Parcel;
import android.os.Parcelable;

public class UserDevice implements Parcelable {
    private String deviceId;
    private String customName;
    private long addedAt;

    public UserDevice() {
    }

    public UserDevice(String deviceId, String customName) {
        this.deviceId = deviceId;
        this.customName = customName != null && !customName.trim().isEmpty() ? customName.trim() : deviceId;
        this.addedAt = System.currentTimeMillis();
    }

    protected UserDevice(Parcel in) {
        deviceId = in.readString();
        customName = in.readString();
        addedAt = in.readLong();
    }

    public static final Creator<UserDevice> CREATOR = new Creator<UserDevice>() {
        @Override
        public UserDevice createFromParcel(Parcel in) {
            return new UserDevice(in);
        }

        @Override
        public UserDevice[] newArray(int size) {
            return new UserDevice[size];
        }
    };

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName != null && !customName.trim().isEmpty() ? customName.trim() : this.deviceId;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceId);
        dest.writeString(customName);
        dest.writeLong(addedAt);
    }

    @Override
    public String toString() {
        return customName;
    }
} 