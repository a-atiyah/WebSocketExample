package com.a.atiyah.websocketexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {
    private final static int STORAGE_REQ_CODE = 10;
    //Constants
    public static String EXTRA_USERNAME_KEY = "username";
    //Declare Variables
    TextInputLayout mTFUsername;
    Button mBtnEnter;
    String[] mStoragePermissions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initialize UI Components
        initUI();
        //Handle Clicks
        mBtnEnter.setOnClickListener(v -> {
            String username = mTFUsername.getEditText().getText().toString().trim();

            if (TextUtils.isEmpty(username) | username == null){
                Toast.makeText(MainActivity.this, getString(R.string.type_user_name), Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra(EXTRA_USERNAME_KEY, username);
            startActivity(intent);
        });
    }

    private void initUI() {
        mTFUsername = findViewById(R.id.tf_msg);
        mBtnEnter = findViewById(R.id.btn_enter);
        mStoragePermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    private boolean checkStoragePermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED));
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, mStoragePermissions, STORAGE_REQ_CODE);
    }
}