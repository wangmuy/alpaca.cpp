package org.alpaca.test;

import java.io.File;

/**
 * Deprecated jni:
 * put android.useDeprecatedNdk=true in gradle.properties
 */
public class NativeUtils {
    static {
        System.loadLibrary("NativeUtils");
    }

    public static final int StatusUnknown = 0;
    public static final int StatusOK = 1;
    public static final int StatusFailed = -1;

    public interface EmitCallback {
        void onEmit(String newStr, int status);
    }

    public static boolean loadModel(File modelPath) {
        return loadModel(modelPath.getAbsolutePath());
    }

    public static native boolean loadModel(String modelPath);
    public static native String getAnswer(String question, EmitCallback cb);
}
