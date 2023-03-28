package org.alpaca.test;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NativeUtils {
    private static final String TAG = "NativeUtils";
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

    private static final List<String> UNIGRAMS = new ArrayList<>(20000);
    private static final Random RAND = new Random();
    public static String getAnswerMock(Context ctx, String question, EmitCallback cb) {
        if (UNIGRAMS.size() == 0) {
            loadUnigrams(ctx);
        }
        int len = Math.max(10, RAND.nextInt(20));
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < len; ++i) {
            String str = UNIGRAMS.get(RAND.nextInt(UNIGRAMS.size()));
            if (i> 0) {
                str = " " + str;
            }
            if (i == len-1) {
                str += ".";
            }
            cb.onEmit(str, 1);
            sb.append(str);
        }
        return sb.toString();
    }

    private static void loadUnigrams(Context ctx) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ctx.getResources().getAssets().open("unigram_20k.txt")))) {
            String line;
            while ((line=reader.readLine()) != null) {
                UNIGRAMS.add(line);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadUnigrams failed", e);
        }
    }
}
