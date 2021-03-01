package com.a.atiyah.websocketexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.FileNotFoundException;
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
    private static final int IMG_REQUEST_CODE = 100;

    // Variables
    private String mUsername = "";
    private WebSocket mWebSocket;
    private final String mUrl = "ws://echo.websocket.org";

    // Views
    RecyclerView mRVMessages;
    ImageButton mIBSend, mIBPickImg;
    TextInputLayout mTFMessage;

    MessageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

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
    }

    private void initUI() {
        mRVMessages = findViewById(R.id.rv_messages);
        mIBSend = findViewById(R.id.ib_send);
        mIBPickImg = findViewById(R.id.ib_gallery);
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
}