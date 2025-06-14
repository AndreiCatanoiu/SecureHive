package licenta.andrei.catanoiu.securehive.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Alert implements Parcelable {
    private String id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String message;
    private Date timestamp;
    private String severity;
    private boolean isRead;

    public Alert() {
    }

    public Alert(String id, String deviceId, String deviceName, String deviceType, 
                 String message, Date timestamp, String severity) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.message = message;
        this.timestamp = timestamp;
        this.severity = severity;
        this.isRead = false;
    }

    protected Alert(Parcel in) {
        id = in.readString();
        deviceId = in.readString();
        deviceName = in.readString();
        deviceType = in.readString();
        message = in.readString();
        timestamp = new Date(in.readLong());
        severity = in.readString();
        isRead = in.readByte() != 0;
    }

    public static final Creator<Alert> CREATOR = new Creator<Alert>() {
        @Override
        public Alert createFromParcel(Parcel in) {
            return new Alert(in);
        }

        @Override
        public Alert[] newArray(int size) {
            return new Alert[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(deviceId);
        dest.writeString(deviceName);
        dest.writeString(deviceType);
        dest.writeString(message);
        dest.writeLong(timestamp.getTime());
        dest.writeString(severity);
        dest.writeByte((byte) (isRead ? 1 : 0));
    }
} 