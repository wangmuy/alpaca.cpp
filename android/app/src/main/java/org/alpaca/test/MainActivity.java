package org.alpaca.test;


import static org.alpaca.test.Const.PREFIX;
import static org.alpaca.test.NativeUtils.StatusFailed;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = PREFIX + "MainActivity";

    private final StringBuilder mSb = new StringBuilder();
    private final HandlerThread mHT = new HandlerThread("ModelThread");

    private static class ModelHandler extends Handler {
        public static final int MSG_LOAD_MODEL = 1;
        public static final int MSG_GET_ANSWER = 2;
        public static final int MSG_SET_MOCK = 3;

        private boolean mock = false;

        public static class LoadModelObj {
            MainActivity activity;
            TextView tv;
            ProgressBar progressBar;
            File file;
        }
        public static class GetAnswerObj {
            String question;
            Consumer<String> answerCb;
            NativeUtils.EmitCallback cb;
            Context appContext;
            ProgressBar progressBar;
        }

        private boolean modelLoaded = false;

        public ModelHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_LOAD_MODEL: {
                    LoadModelObj obj = (LoadModelObj) msg.obj;
                    File file = obj.file;
                    String str;
                    if (mock) {
                        str = "loadModel but set to mock, ignore";
                    } else {
                        if (!modelLoaded) {
                            modelLoaded = NativeUtils.loadModel(file);
                            str = "loadModel ret=" + modelLoaded;
                        } else {
                            str = "loadModel already loaded";
                        }
                    }
                    obj.tv.post(() -> {
                        obj.progressBar.setVisibility(View.INVISIBLE);
                        obj.tv.setText(str);
                    });
                    Log.d(TAG, str);
                }
                break;
                case MSG_GET_ANSWER: {
                    GetAnswerObj obj = (GetAnswerObj) msg.obj;
                    String question = obj.question;
                    String answer;
                    if (mock) {
                        answer = NativeUtils.getAnswerMock(obj.appContext, question, obj.cb);
                    } else {
                        if (!modelLoaded) {
                            obj.cb.onEmit("Please load model first.", StatusFailed);
                            obj.progressBar.setVisibility(View.INVISIBLE);
                            return;
                        }
                        answer = NativeUtils.getAnswer(question, obj.cb);
                    }
                    Log.d(TAG, "getAnswer ret=" + answer);
                    if (obj.answerCb != null) {
                        obj.answerCb.accept(answer);
                    }
                    obj.progressBar.setVisibility(View.INVISIBLE);
                }
                break;
                case MSG_SET_MOCK: {
                    mock = (boolean) msg.obj;
                }
                break;
            }
        }
    }
    private Handler mModelHandler;
    private static final int REQ_READ_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHT.start();
        mModelHandler = new ModelHandler(mHT.getLooper());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        ChatItem.CHATS.clear();
        ChatAdapter adapter = new ChatAdapter(ChatItem.CHATS);
        recyclerView.setAdapter(adapter);

        TextView tv = findViewById(R.id.textview);
        ProgressBar progressBar = findViewById(R.id.progress);

        Button btnLoadModel = findViewById(R.id.btnLoadModel);
        btnLoadModel.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            Message msg = Message.obtain();
            msg.what = ModelHandler.MSG_LOAD_MODEL;
            ModelHandler.LoadModelObj obj = new ModelHandler.LoadModelObj();
            obj.activity = MainActivity.this;
            obj.tv = tv;
            obj.progressBar = progressBar;
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            obj.file = new File(downloadDir, "ggml-alpaca-7b-q4.bin");
            msg.obj = obj;
            mModelHandler.sendMessage(msg);
        });

        SwitchCompat mockSwitch = findViewById(R.id.mockSwitch);
        mockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Message msg = Message.obtain();
            msg.what = ModelHandler.MSG_SET_MOCK;
            msg.obj = isChecked;
            mModelHandler.sendMessage(msg);
        });

        EditText editQuestion = findViewById(R.id.editQuestion);

        Button btnGetAnswer = findViewById(R.id.btnGetAnswer);
        btnGetAnswer.setOnClickListener(v -> {
            if (mSb.length() > 0) {
                mSb.delete(0, mSb.length());
            }
            progressBar.setVisibility(View.VISIBLE);
            tv.setText("");
            String question = editQuestion.getText().toString();
            editQuestion.setText("");
            if (question.trim().length() == 0) {
                tv.setText("please input something other than blanks");
                return;
            }

            ChatItem questionItem = new ChatItem();
            questionItem.name = ChatItem.NAME_ME;
            questionItem.sentence = question;
            ChatItem.CHATS.add(0, questionItem);
            adapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);

            Message msg = Message.obtain();
            msg.what = ModelHandler.MSG_GET_ANSWER;
            ModelHandler.GetAnswerObj obj = new ModelHandler.GetAnswerObj();
            obj.question = question;
            obj.appContext = getApplicationContext();
            obj.cb = (NativeUtils.EmitCallback) (newStr, status) -> {
                Log.d(TAG, "onEmit: newStr=" + newStr + ", status=" + status);
                mSb.append(newStr);
                String soFar = mSb.toString();
                runOnUiThread(() -> {
                    tv.setText(soFar);
                });
            };
            obj.answerCb = answer -> {
                runOnUiThread(() -> {
                    tv.setText("");
                    ChatItem answerItem = new ChatItem();
                    answerItem.name = ChatItem.NAME_BOT;
                    answerItem.sentence = answer;
                    ChatItem.CHATS.add(0, answerItem);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);
                });
            };
            obj.progressBar = progressBar;
            msg.obj = obj;
            mModelHandler.sendMessage(msg);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{perm}, REQ_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_EXTERNAL_STORAGE
                && (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)) {
            Toast.makeText(this, "Loading model in downloads requires external storage permission", Toast.LENGTH_SHORT).show();
        }
    }
}
