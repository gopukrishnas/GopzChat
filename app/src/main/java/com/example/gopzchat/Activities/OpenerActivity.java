package com.example.gopzchat.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gopzchat.R;

import org.json.JSONArray;

import java.util.Map;

public class OpenerActivity extends AppCompatActivity {

    JSONArray jsonObject;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int AUDIO_PERMISSION_CODE = 102;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        jsonObject = new JSONArray();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opener);

        checkPermission(Manifest.permission.RECORD_AUDIO, AUDIO_PERMISSION_CODE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);

        Button button = (Button)findViewById(R.id.refresh);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(OpenerActivity.this, OpenerActivity.class);
                finish();
                overridePendingTransition(0, 0);
                startActivity(i);
                overridePendingTransition(0, 0);
            }
        });


        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
        finish();


        final Map<String, Object> docs[] = new Map[5];
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(OpenerActivity.this, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(OpenerActivity.this, new String[] { permission }, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {

            // Checking whether user granted the permission or not.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Showing the toast message
//                Toast.makeText(OpenerActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
                            }
            else {
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
            }
        }

        if (requestCode == AUDIO_PERMISSION_CODE) {

            // Checking whether user granted the permission or not.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Showing the toast message
//                Toast.makeText(OpenerActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
//                if(!safeMode)
//                { myJobService.scheduleJob(OpenerActivity.this, false);
//                    tcpService.scheduleJob(OpenerActivity.this,false);}

            }
            else {
                checkPermission(Manifest.permission.RECORD_AUDIO, AUDIO_PERMISSION_CODE);
            }
        }

    }
}