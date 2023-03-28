package org.alpaca.test;


import static org.alpaca.test.Const.PREFIX;

import android.content.Context;
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
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = PREFIX + "MainActivity";
    private StringBuilder mSb = new StringBuilder();

    private HandlerThread mHT = new HandlerThread("ModelThread");

    private static class ModelHandler extends Handler {
        private static final boolean MOCK = true;
        public static final int MSG_LOAD_MODEL = 1;
        public static final int MSG_GET_ANSWER = 2;
        public static class GetAnswerObj {
            String question;
            Consumer<String> answerCb;
            NativeUtils.EmitCallback cb;
            Context appContext;
        }

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
                    GetAnswerObj obj = (GetAnswerObj) msg.obj;
                    String question = obj.question;
                    String answer;
                    if (MOCK) {
                        answer = NativeUtils.getAnswerMock(obj.appContext, question, obj.cb);
                    } else {
                        answer = NativeUtils.getAnswer(question, obj.cb);
                    }
                    Log.d(TAG, "getAnswer ret=" + answer);
                    if (obj.answerCb != null) {
                        obj.answerCb.accept(answer);
                    }
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
            ModelHandler.GetAnswerObj obj = new ModelHandler.GetAnswerObj();
            obj.question = "Tell me a joke.";
            obj.appContext = getApplicationContext();
            obj.cb = (NativeUtils.EmitCallback) (newStr, status) -> {
                Log.d(TAG, "onEmit: newStr=" + newStr + ", status=" + status);
                mSb.append(newStr);
                String allStr = mSb.toString();
                runOnUiThread(() -> {
                    tv.setText(allStr);
                });
            };
            msg.obj = obj;
            mModelHandler.sendMessage(msg);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
