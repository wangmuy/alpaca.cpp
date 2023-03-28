package org.alpaca.test;


import static org.alpaca.test.Const.PREFIX;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = PREFIX + "MainActivity";
    private StringBuilder mSb = new StringBuilder();

    private HandlerThread mHT = new HandlerThread("ModelThread");

    private static class ModelHandler extends Handler {
        public static final int MSG_LOAD_MODEL = 1;
        public static final int MSG_GET_ANSWER = 2;
        private boolean modelLoaded = false;

        public ModelHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_LOAD_MODEL:
                    modelLoaded = NativeUtils.loadModel((File) msg.obj);
                    Log.d(TAG, "loadModel ret=" + modelLoaded);
                    break;
                case MSG_GET_ANSWER:
                    NativeUtils.EmitCallback cb = (NativeUtils.EmitCallback) msg.obj;
                    String answer = NativeUtils.getAnswer("What is the meaning of life?", cb);
                    Log.d(TAG, "getAnswer ret=" + answer);
                    break;
            }
        }
    }
    private Handler mModelHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHT.start();
        mModelHandler = new ModelHandler(mHT.getLooper());

        TextView tv = (TextView)findViewById(R.id.textview);
        Button btnLoadModel = (Button)findViewById(R.id.btnLoadModel);
        btnLoadModel.setOnClickListener(v -> {
            Message msg = Message.obtain();
            msg.what = ModelHandler.MSG_LOAD_MODEL;
            msg.obj = new File(getFilesDir(), "ggml-alpaca-7b-q4.bin");
            mModelHandler.sendMessage(msg);
        });
        Button btnGetAnswer = (Button)findViewById(R.id.btnGetAnswer);
        btnGetAnswer.setOnClickListener(v -> {
            Message msg = Message.obtain();
            msg.what = ModelHandler.MSG_GET_ANSWER;
            msg.obj = (NativeUtils.EmitCallback) (newStr, status) -> {
                Log.d(TAG, "onEmit: newStr=" + newStr + ", status=" + status);
                mSb.append(newStr);
                String allStr = mSb.toString();
                runOnUiThread(() -> {
                    tv.setText(allStr);
                });
            };
            mModelHandler.sendMessage(msg);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
