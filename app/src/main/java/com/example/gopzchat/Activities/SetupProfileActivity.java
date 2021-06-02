package com.example.gopzchat.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.gopzchat.Models.User;
import com.example.gopzchat.databinding.ActivitySetupProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class SetupProfileActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    public static final int PERMISSIONS_REQUEST_READ_CAMERA = 2;
    public static final int PERMISSIONS_REQUEST_READ_STORAGE = 3;
    public static final int PERMISSIONS_REQUEST_WRITE_STORAGE = 4;

    ActivitySetupProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage, compressedImage;
    ProgressDialog dialog;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getContactPermission();
        getCameraPermission();
//        getStoragePermissionRead();
//        getStoragePermissionWrite();

        getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Creating Profile..");
        dialog.setCancelable(false);

        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);
                //When we start another activity from current activity to get the result for it, we call the method startActivityForResult(intent, RESPONSE_CODE);.
                // It redirects to another activity like opens camera, gallery, etc.
                // After taking image from gallery or camera then come back to current activity first method that calls is onActivityResult(int requestCode, int resultCode, Intent data).
                // We get the result in this method like taken image from camera or gallery.
            }
        });

        binding.confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.nameBox.getText().toString();

                if(name.isEmpty())
                {
                    binding.nameBox.setError("Please enter your name");
                }
                dialog.show();

//                if(selectedImage != null)
//                {
//                    file = new File(SiliCompressor.with(SetupProfileActivity.this)
//                        .compress(FileUtils.getPath(SetupProfileActivity.this, selectedImage),
//                                new File(SetupProfileActivity.this.getCacheDir(), "temp"), true));
//                    compressedImage = Uri.fromFile(file);
//                    Log.e("nash", "compressed "+ compressedImage);
//                }


                if(selectedImage != null)
                {

                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(SetupProfileActivity.this.getContentResolver(), selectedImage);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    System.out.println("fullsizebp");

                    Bitmap reducedBp = ImageResizer.reduceBitmapSize(bitmap, 1036800);
                    System.out.println("reducedBp");

                    File reducedFile = getBitMapFile(reducedBp);
                    System.out.println("reducedfile");

                    Uri selectedImage = Uri.fromFile(reducedFile);


//                    Log.e("nash", "notnull "+ compressedImage);
                    StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                            Log.e("nash", "on complete");
                            if(task.isSuccessful()) {
//                                Log.e("nash", "task success ");
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
//                                        Log.e("nash", "on success ");
                                        String imageUrl = uri.toString();
                                        String uid = auth.getUid();
                                        String phoneNumber = auth.getCurrentUser().getPhoneNumber();
                                        String name = binding.nameBox.getText().toString();

                                        User user = new User(uid, name, phoneNumber, imageUrl);

                                        database.getReference()
                                                .child("users")
                                                .child(uid)
                                                .setValue(user)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
//                                                        Log.e("nash", "on db success ");
                                                        dialog.dismiss();
                                                        Intent intent = new Intent(SetupProfileActivity.this, MainActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(intent);
                                                        finish(); // to avoid going back when back button is pressed
                                                    }
                                                });

                                    }
                                });
                            }
                        }
                    });
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Please upload profile picture..",Toast.LENGTH_LONG).show();
                    String uid = auth.getUid();
                    String phoneNumber = auth.getCurrentUser().getPhoneNumber();

                    User user = new User(uid, name, phoneNumber, "No Image");

                    database.getReference()
                            .child("users")
                            .child(uid)
                            .setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(SetupProfileActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish(); // to avoid going back when back button is pressed
                                }
                            });
                }

            }
        });

    }

    private File getBitMapFile(Bitmap reducedBp) {
//        File file = new File(Environment.getExternalStorageDirectory()+File.separator+"reduced_file");
        File file = new File(SetupProfileActivity.this.getFilesDir(), "text");
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        reducedBp.compress(Bitmap.CompressFormat.JPEG, 80, bo);
        byte[] bitmapdata = bo.toByteArray();

        try{
            file.createNewFile();
            FileOutputStream fo = new FileOutputStream(file);
            fo.write(bitmapdata);
            fo.flush();
            fo.close();
            return file;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return file;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null)
        {
            if(data.getData() != null) {
                binding.imageView.setImageURI(data.getData());
                selectedImage = data.getData();
            }
        }
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