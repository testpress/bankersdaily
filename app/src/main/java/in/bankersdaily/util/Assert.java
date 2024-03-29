package in.bankersdaily.util;


import androidx.annotation.NonNull;

public class Assert {

    public static void assertNotNull(@NonNull String message, Object object) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void assertNotNullAndNotEmpty(@NonNull String message, String string) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
