package com.a.atiyah.websocketexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.a.atiyah.websocketexample.adapter.MessageAdapter;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatActivity extends AppCompatActivity {
    // Constants
    private static final String LOG_TAG = ChatActivity.class.getSimpleName();
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int IMG_REQUEST_CODE = 100;
    // Variables
    private static String fileName = null;
    private String mUsername = "";
    private WebSocket mWebSocket;
    private final String mUrl = "ws://echo.websocket.org";
    // Views
    RecyclerView mRVMessages;
    ImageButton mIBSend, mIBPickImg, mIBRecord, mIBPly;
    TextInputLayout mTFMessage;
    MessageAdapter mAdapter;
    boolean mStartRecording = false;
    boolean mStartPlaying = false;

    /*** Audio ***/
    private static final String AUDIO_RECORDER_FILE_EXT_3GP = ".3gp";
    private static final String AUDIO_RECORDER_FILE_EXT_MP4 = ".mp4";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private MediaRecorder recorder = null;
    private int currentFormat = 0;
    private int output_formats[] = { MediaRecorder.OutputFormat.MPEG_4,MediaRecorder.OutputFormat.THREE_GPP };
    private String file_exts[] = { AUDIO_RECORDER_FILE_EXT_MP4, AUDIO_RECORDER_FILE_EXT_3GP };

    private String recordPath;
    private String recordUrl;
    private MediaPlayer player = null;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    String _audioBase64 = null;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
            mStartRecording = false;
        } else {
            stopRecording();
            mStartRecording = true;
        }
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        Toast.makeText(this, "Start Recording:" + fileName, Toast.LENGTH_LONG).show();

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        Toast.makeText(this, "Stop Recording", Toast.LENGTH_LONG).show();
        recorder = null;
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        encodeAudio(fileName);
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Record to the external cache directory for visibility
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        //Get Intent
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(MainActivity.EXTRA_USERNAME_KEY)){
                mUsername = intent.getStringExtra(MainActivity.EXTRA_USERNAME_KEY);
            }
        }

        //Initialize UI Components
        initUI();
        //Initialize WebSocket
        initWebSocket();
        //Handle Clicks
        mIBSend.setOnClickListener(v -> {
            if (TextUtils.isEmpty(Objects.requireNonNull(mTFMessage.getEditText()).getText().toString().trim()) |
                    mTFMessage.getEditText().getText().toString().trim() == null) {
                Toast.makeText(ChatActivity.this, getString(R.string.type_msg), Toast.LENGTH_SHORT).show();
                return;
            }
            sendMsg();
        });

        mIBPickImg.setOnClickListener(v -> {
            pickImgFromGallery();
        });

        mIBRecord.setOnClickListener(v -> {
            if (!mStartRecording) {
                onRecord(true);
                mStartRecording = true;
            }
            else {
                onRecord(false);
                mStartRecording = false;
            }
        });

        mIBPly.setOnClickListener(v -> {
            if (!mStartPlaying) {
                startPlaying();
                mStartPlaying = true;
            }
            else {
                stopPlaying();
                mStartPlaying = false;
            }
        });
    }

    private void initUI() {
        mRVMessages = findViewById(R.id.rv_messages);
        mIBSend = findViewById(R.id.ib_send);
        mIBPickImg = findViewById(R.id.ib_gallery);
        mIBRecord = findViewById(R.id.ib_mic);
        mIBPly = findViewById(R.id.ib_ply);
        mTFMessage = findViewById(R.id.tf_msg);

        mAdapter = new MessageAdapter(this);
        mRVMessages.setAdapter(mAdapter);
        mRVMessages.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initWebSocket() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(mUrl).build();
        mWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                super.onMessage(webSocket, text);
                runOnUiThread(() -> {
                    try{
                        JSONObject obj = new JSONObject(text);
                        obj.put("isSent", false);
                        mAdapter.setItem(obj);
                        mRVMessages.smoothScrollToPosition(mAdapter.getItemCount() -1);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                super.onOpen(webSocket, response);
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, getString(R.string.connected), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void sendMsg() {
        JSONObject obj = new JSONObject();

        try{
            obj.put("username", mUsername);
            obj.put("message", Objects.requireNonNull(mTFMessage.getEditText()).getText().toString().trim());
            obj.put("isSent", true);
            mWebSocket.send(obj.toString());

            mAdapter.setItem(obj);
            mRVMessages.smoothScrollToPosition(mAdapter.getItemCount() -1);
            clearTheFields();
        } catch (Exception e) {
            Log.e(LOG_TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    private void clearTheFields() {
        Objects.requireNonNull(mTFMessage.getEditText()).setText("");
    }

    private void pickImgFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_img)), IMG_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMG_REQUEST_CODE && resultCode == RESULT_OK){
            try {
                InputStream input = getContentResolver().openInputStream(Objects.requireNonNull(data).getData());
                // Create Bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                sendImg(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendImg(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, output);
        String base64String = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);

        JSONObject obj = new JSONObject();
        try {
            obj.put("username", mUsername);
            obj.put("image", base64String);

            mWebSocket.send(obj.toString());

            obj.put("isSent", true);
            mAdapter.setItem(obj);
            mRVMessages.smoothScrollToPosition(mAdapter.getItemCount() -1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private void encodeAudio(String selectedPath) {

        byte[] audioBytes;
        try {

            // Just to check file size.. Its is correct i-e; Not Zero
            File audioFile = new File(selectedPath);
            long fileSize = audioFile.length();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(new File(selectedPath));
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);
            audioBytes = baos.toByteArray();

            // Here goes the Base64 string
            _audioBase64 = Base64.encodeToString(audioBytes, Base64.DEFAULT);
            Log.d(LOG_TAG, "encodeAudio:::" + _audioBase64);
            sendAudio(_audioBase64);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendAudio(String base64String) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("username", mUsername);
            obj.put("audio", base64String);

            mWebSocket.send(obj.toString());

            obj.put("isSent", true);
            mAdapter.setItem(obj);
            mRVMessages.smoothScrollToPosition(mAdapter.getItemCount() -1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void decodeAudio(String base64AudioData, File fileName, String path, MediaPlayer mp) {
        try {

            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(Base64.decode(base64AudioData.getBytes(), Base64.DEFAULT));
            fos.close();

            try {
                mp = new MediaPlayer();
                mp.setDataSource(path);
                mp.prepare();
                mp.start();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}