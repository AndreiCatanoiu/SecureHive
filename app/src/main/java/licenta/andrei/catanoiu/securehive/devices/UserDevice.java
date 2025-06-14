package licenta.andrei.catanoiu.securehive.devices;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;

public class UserDevice implements Parcelable {
    private String deviceId;
    private String customName;
    private Timestamp addedAt;

    public UserDevice() {
        // Required empty constructor for Firestore
    }

    public UserDevice(String deviceId, String customName) {
        this.deviceId = deviceId;
        this.customName = customName != null && !customName.trim().isEmpty() ? customName.trim() : deviceId;
        this.addedAt = Timestamp.now();
    }

    protected UserDevice(Parcel in) {
        deviceId = in.readString();
        customName = in.readString();
        long seconds = in.readLong();
        int nanoseconds = in.readInt();
        addedAt = new Timestamp(seconds, nanoseconds);
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

    public Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Timestamp addedAt) {
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
        dest.writeLong(addedAt != null ? addedAt.getSeconds() : 0);
        dest.writeInt(addedAt != null ? addedAt.getNanoseconds() : 0);
    }

    @Override
    public String toString() {
        return customName;
    }
} 