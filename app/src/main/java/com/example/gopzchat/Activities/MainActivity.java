package com.example.gopzchat.Activities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gopzchat.Adapters.TopStatusAdapter;
import com.example.gopzchat.Models.Status;
import com.example.gopzchat.Models.UserFiles;
import com.example.gopzchat.Models.UserStatus;
import com.example.gopzchat.Notification.MyFirebaseMessagingService;
import com.example.gopzchat.R;
import com.example.gopzchat.Models.User;
import com.example.gopzchat.Adapters.UsersAdapter;
import com.example.gopzchat.databinding.ActivityMainBinding;
import com.github.tamir7.contacts.Contact;
import com.github.tamir7.contacts.Contacts;
import com.github.tamir7.contacts.Query;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import android.content.Context;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    public static final int PERMISSIONS_REQUEST_READ_CAMERA = 2;
    public static final int PERMISSIONS_REQUEST_READ_STORAGE = 3;
    public static final int PERMISSIONS_REQUEST_WRITE_STORAGE = 4;
    public static final String MyPREFERENCES = "gopzchat" ;
    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdapter usersAdapter;
    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog dialog;
    User user;
    List<String> friendsNumberList = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getContactPermission();
        getCameraPermission();
        getStoragePermissionRead();
        getStoragePermissionWrite();
        Contacts.initialize(this);

        database = FirebaseDatabase.getInstance();

//        ConstraintLayout layout = findViewById(R.id.layout_main);
//        AnimationDrawable animationDrawable = (AnimationDrawable) layout.getBackground();
//        animationDrawable.setEnterFadeDuration(2000);
//        animationDrawable.setExitFadeDuration(4000);
//        animationDrawable.start();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<String> task) {
                if(task.isSuccessful()){
                    String token = task.getResult();
                    database.getReference().child("token").child(FirebaseAuth.getInstance().getUid()).setValue(token);
                }
            }
        });

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image..");
        dialog.setCancelable(false);


        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        usersAdapter = new UsersAdapter(this,users);
        statusAdapter = new TopStatusAdapter(this, userStatuses);
//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);
        binding.statusList.setAdapter(statusAdapter);
        binding.recyclerView.setAdapter(usersAdapter);
        binding.recyclerView.showShimmerAdapter();
        binding.statusList.showShimmerAdapter();



        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                String currentUserId = FirebaseAuth.getInstance().getUid();
                String currentUserName = "";
                String currentUserNumber = "";

                Query q = Contacts.getQuery();
                q.hasPhoneNumber();
                List<Contact> contacts = q.find();

                List<String> numberList = new ArrayList<String>();

                for(int index=0; index<contacts.size(); index++)
                {
                    Contact tempCont = contacts.get(index);
                    for(int j=0; j<tempCont.getPhoneNumbers().size(); j++)
                    {
                        String tempNum = tempCont.getPhoneNumbers().get(j).getNormalizedNumber();
                        if(tempNum != null){
                            numberList.add(tempNum);
                        }
                    }
                }

                for(DataSnapshot snapshot1: snapshot.getChildren()){
                    User user = snapshot1.getValue(User.class);
                    String num = user.getPhoneNumber();

                    if(!currentUserId.equals(user.getUid())){   //add all users other than current user
                        if(numberList.contains(num)){ // if user is present in contact list
                            users.add(user);
                            friendsNumberList.add(user.getPhoneNumber());
                        }
                    }
                    else {
                        currentUserName = user.getName(); // to get name of current user
                        currentUserNumber = user.getPhoneNumber(); // to get number of current user
                        SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                        sharedpreferences.edit().putString("currentUserName", currentUserName).apply();
                        sharedpreferences.edit().putString("currentUserNumber", currentUserNumber).apply();
                        friendsNumberList.add(currentUserNumber);
                    }

                }
                binding.recyclerView.hideShimmerAdapter();
                binding.statusList.hideShimmerAdapter();

                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    userStatuses.clear();
                    for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                        String phNumber = storySnapshot.child("phoneNumber").getValue(String.class);
                        if(friendsNumberList.contains(phNumber))
                        {
                            UserStatus status = new UserStatus();
                            status.setName(storySnapshot.child("name").getValue(String.class));
                            status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                            status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));
                            status.setPhoneNumber(phNumber);

                            ArrayList<Status> statuses = new ArrayList<>();
                            for(DataSnapshot statusSnapshot: storySnapshot.child("statuses").getChildren()){
                                Status sampleStatus = statusSnapshot.getValue(Status.class);
                                statuses.add(sampleStatus);
                            }

                            status.setStatuses(statuses);

                            userStatuses.add(status);
                        }

                    }
                    binding.statusList.hideShimmerAdapter();
                    statusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent, 75);
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            if(data.getData() != null)
            {
                dialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child("status")
                        .child(date.getTime()+"");
                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(user.getName());
                                    userStatus.setProfileImage(user.getProfileImage());
                                    userStatus.setLastUpdated(date.getTime());
                                    userStatus.setPhoneNumber(user.getPhoneNumber());

                                    HashMap<String, Object> obj = new HashMap<>();
                                    obj.put("name", userStatus.getName());
                                    obj.put("profileImage", userStatus.getProfileImage());
                                    obj.put("lastUpdated", userStatus.getLastUpdated());
                                    obj.put("phoneNumber", userStatus.getPhoneNumber());

                                    String imageUrl = uri.toString();
                                    Status status = new Status(imageUrl, userStatus.getLastUpdated());

                                    database.getReference().child("stories").child(FirebaseAuth.getInstance().getUid())
                                            .updateChildren(obj);

                                    database.getReference().child("stories").child(FirebaseAuth.getInstance().getUid())
                                            .child("statuses")
                                            .push()
                                            .setValue(status);

                                    dialog.dismiss();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence")
                .child(currentId)
                .setValue("Online");
    }

    @Override
    protected void onPause() {
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence")
                .child(currentId)
                .setValue(Calendar.getInstance().getTimeInMillis()+"");
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case (R.id.search):
                Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show();
//                startActivity(new Intent(MainActivity.this, GroupChatActivity.class));
                break;
            case (R.id.settings):
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                break;

            case (R.id.contact):
                Uri u = Uri.parse("tel:" + "+918547367853");
                Intent i = new Intent(Intent.ACTION_DIAL, u);
                try{
                    startActivity(i);
                }
                catch (SecurityException s)
                {
                    Toast.makeText(this, "An error occurred", Toast.LENGTH_LONG)
                            .show();
                }
                break;

            case (R.id.introConfig):
                SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                boolean intro = sharedpreferences.getBoolean("intro", true);
                if(intro)
                {
                    sharedpreferences.edit().putBoolean("intro",false).apply();
                    Toast.makeText(this, "Intro disabled", Toast.LENGTH_SHORT)
                            .show();
                }
                else {
                    sharedpreferences.edit().putBoolean("intro",true).apply();
                    Toast.makeText(this, "Intro enabled", Toast.LENGTH_SHORT)
                            .show();
                }
                break;


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void getContactPermission() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_CONTACTS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Read Contacts permission");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setMessage("Please enable access to contacts.");
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(
                                new String[]
                                        {android.Manifest.permission.READ_CONTACTS}
                                , PERMISSIONS_REQUEST_READ_CONTACTS);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_CONTACTS},
                        PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
    }

    private void getCameraPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.CAMERA)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Camera permission");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setMessage("Please grant access to camera.");
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(
                                new String[]
                                        {Manifest.permission.CAMERA}
                                , PERMISSIONS_REQUEST_READ_CAMERA);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA},
                        PERMISSIONS_REQUEST_READ_CAMERA);
            }
        }
    }

    private void getStoragePermissionRead() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Storage permission");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setMessage("Please grant access to storage.");
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(
                                new String[]
                                        {Manifest.permission.READ_EXTERNAL_STORAGE}
                                , PERMISSIONS_REQUEST_READ_STORAGE);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_STORAGE);
            }
        }
    }

    private void getStoragePermissionWrite() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Storage permission");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setMessage("Please grant access to storage.");
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(
                                new String[]
                                        {Manifest.permission.WRITE_EXTERNAL_STORAGE}
                                , PERMISSIONS_REQUEST_WRITE_STORAGE);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_STORAGE);
            }
        }
    }



}

//    Alias name: androiddebugkey
//        Creation date: 28-Oct-2020
//        Entry type: PrivateKeyEntry
//        Certificate chain length: 1
//        Certificate[1]:
//        Owner: C=US, O=Android, CN=Android Debug
//        Issuer: C=US, O=Android, CN=Android Debug
//        Serial number: 1
//        Valid from: Wed Oct 28 10:28:04 IST 2020 until: Fri Oct 21 10:28:04 IST 2050
//        Certificate fingerprints:
//        SHA1: F8:A7:E5:91:36:52:EC:57:D5:F1:4E:2A:CB:E8:44:16:49:BE:40:FA
//        SHA256: 74:75:4C:99:8B:4E:8E:62:BE:80:73:80:50:89:4C:C1:FA:0C:86:EC:D9:5A:86:80:38:0F:8C:A0:81:A2:FF:3E
//        Signature algorithm name: SHA1withRSA
//        Subject Public Key Algorithm: 2048-bit RSA key
//        Version: 1
//
//        Warning:
//        The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format using "keytool -importkeystore -srckeystore debug.keystore -destkeystore debug.keystore -deststoretype pkcs12".