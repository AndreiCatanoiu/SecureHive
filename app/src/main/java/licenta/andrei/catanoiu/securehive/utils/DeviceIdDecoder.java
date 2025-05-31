package licenta.andrei.catanoiu.securehive.utils;

import android.util.Base64;
import android.util.Log;

public class DeviceIdDecoder {
    private static final String TAG = "DeviceIdDecoder";
    private static final byte[] KEY = {0x5A, (byte)0xA5, 0x3C, (byte)0xC3};

    public static String decodeDeviceId(String encodedId) {
        try {
            // Decodăm Base64
            byte[] decodedBytes = Base64.decode(encodedId, Base64.DEFAULT);
            
            // Aplicăm XOR cu cheia
            byte[] result = new byte[decodedBytes.length];
            for (int i = 0; i < decodedBytes.length; i++) {
                result[i] = (byte)(decodedBytes[i] ^ KEY[i % KEY.length]);
            }
            
            // Convertim la String
            String decodedId = new String(result);
            Log.d(TAG, "Decoded ID: " + decodedId + " from: " + encodedId);
            return decodedId;
        } catch (Exception e) {
            Log.e(TAG, "Error decoding device ID: " + encodedId, e);
            return encodedId;
        }
    }

    public static DeviceType getDeviceType(String encodedId) {
        String decodedId = decodeDeviceId(encodedId);
        if (decodedId.contains("PIR")) {
            return DeviceType.PIR;
        } else if (decodedId.contains("Gaz")) {
            return DeviceType.GAS;
        }
        return DeviceType.UNKNOWN;
    }

    public enum DeviceType {
        PIR,
        GAS,
        UNKNOWN
    }
} 