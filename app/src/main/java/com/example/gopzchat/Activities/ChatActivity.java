package com.example.gopzchat.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.gopzchat.Adapters.MessagesAdapter;
import com.example.gopzchat.Models.Message;
import com.example.gopzchat.Notification.FcmNotificationsSender;
import com.example.gopzchat.R;
import com.example.gopzchat.databinding.ActivityChatBinding;
import com.fxn.pix.Options;
import com.fxn.pix.Pix;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    String senderRoom;
    String receiverRoom;
    FirebaseDatabase database;
    String senderUid;
    String receiverUid;
    String receiverToken;
    FirebaseStorage storage;
    ProgressDialog dialog;
    String onlineStatus = "";
    Boolean notifiedInThisSession;
    public static final String MyPREFERENCES = "gopzchat" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        ConstraintLayout layout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) layout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image..");
        dialog.setCancelable(false);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        notifiedInThisSession = false;

        String profile = getIntent().getStringExtra("image");
        String name = getIntent().getStringExtra("name");
        receiverUid = getIntent().getStringExtra("uid");

        binding.name.setText(name);
        Glide.with(ChatActivity.this).load(profile)
                .placeholder(R.drawable.avatar)
                .into(binding.profile);
        senderUid = FirebaseAuth.getInstance().getUid();
        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        messages = new ArrayList<>();
        adapter = new MessagesAdapter(this,messages, senderRoom, receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        // online status
        database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    String status = snapshot.getValue(String.class);
                    if(!status.isEmpty()){
                        onlineStatus = status;
                        if(status.equals("Offline")){
                            binding.status.setVisibility(View.GONE);
                        }
                        else if(status.equals("Online")){
                            binding.status.setText(status);
                            binding.status.setVisibility(View.VISIBLE);
                        }
                        else {

                            String statusTime = getFormattedDate(ChatActivity.this, Long.parseLong(status));
                            binding.status.setText(statusTime);
                            System.out.println(statusTime);
                            binding.status.setVisibility(View.VISIBLE);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        // receiver token
        database.getReference().child("token").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    receiverToken = snapshot.getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        // load chats
        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for(DataSnapshot snapshot1:snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                            binding.recyclerView.scrollToPosition((messages.size()-1));

                            HashMap<String,Object> lastMsgObj = new HashMap<>();
                            lastMsgObj.put("read", true);  // read status to false only for receiver
                            database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);

                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = binding.messageBox.getText().toString();

                if(messageText.isEmpty())
                {
                    Toast.makeText(ChatActivity.this, "Please type a message..", Toast.LENGTH_LONG).show();
                    return;
                }

                Date date = new Date();
                Message message = new Message(messageText, senderUid, date.getTime());
                binding.messageBox.setText("");
                String randomKey = database.getReference().push().getKey(); //to get a uuid for pushed msg. use this same id for msg in sender and receiver data

                String msg = message.getMessage();
                String substr = msg;

                if(msg.length() > 24)
                {
                    substr = msg.substring(0, 24) + "...";
                }

                HashMap<String,Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg", substr);
                lastMsgObj.put("lastMsgTime", date.getTime());
                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                lastMsgObj.put("read", false);  // read status to false only for receiver
                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);


                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
//                        .push() //push method generates a unique key everytime a new child is added to the specified firebase reference.
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        database.getReference().child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                if(onlineStatus.equals("Online") || notifiedInThisSession)
                                {
                                    // no need to send notifications if user is online
                                    // no need to notify if notified for previous message
                                }
                                else{
                                    SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                                    String currentUserName = sharedpreferences.getString("currentUserName", "");
                                    String body = "New message received";
                                    if(!currentUserName.isEmpty())
                                    {
                                        body = "New message from " + currentUserName;
                                    }
                                    String title = "GopzChat";
                                    FcmNotificationsSender sender = new FcmNotificationsSender(receiverToken, title, body, getApplicationContext(), ChatActivity.this);
                                    sender.SendNotifications();
                                    notifiedInThisSession = true;
                                    }
                            }
                        });
                    }
                });
            }
        });

        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 25);
            }
        });

        binding.camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Options options = Options.init()
                        .setRequestCode(100)                                           //Request code for activity results
                        .setCount(3)                                                   //Number of images to restict selection count
                        .setFrontfacing(false)                                         //Front Facing camera on start
                        //.setPreSelectedUrls(returnValue)                               //Pre selected Image Urls
                        .setSpanCount(4)                                               //Span count for gallery min 1 & max 5
                        .setMode(Options.Mode.Picture)                                     //Option to select only pictures or videos or both
                        .setVideoDurationLimitinSeconds(5)                            //Duration for video recording
                        .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT)     //Orientation
                        .setPath("/pix/images");                                       //Custom Path For media Storage
                Pix.start(ChatActivity.this, options);

            }
        });



        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setTitle(name);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // to enable back button

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==25){
            //send image
            if(data!=null){
                if(data.getData()!=null)
                {
                    dialog.show();
                    Uri tempImage = data.getData();
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), tempImage);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    System.out.println("fullsizebp");

                    Bitmap reducedBp = ImageResizer.reduceBitmapSize(bitmap, 2073600);
                    System.out.println("reducedBp");

                    File reducedFile = getBitMapFile(reducedBp);
                    System.out.println("reducedfile");

                    Uri selectedImage = Uri.fromFile(reducedFile);
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats")
                            .child(calendar.getTimeInMillis()+"");
//                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filePath = uri.toString();
                                        String messageText = binding.messageBox.getText().toString();
                                        Date date = new Date();
                                        Message message = new Message(messageText, senderUid, date.getTime());
                                        message.setImageUrl(filePath);
                                        message.setMessage("photo");
                                        binding.messageBox.setText("");
                                        String randomKey = database.getReference().push().getKey(); //to get a uuid for pushed msg. use this same id for msg in sender and receiver data

                                        HashMap<String,Object> lastMsgObj = new HashMap<>();
                                        lastMsgObj.put("lastMsg", message.getMessage());
                                        lastMsgObj.put("lastMsgTime", date.getTime());
                                        database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                        lastMsgObj.put("read", false);  // read status to false only for receiver
                                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);


                                        database.getReference().child("chats")
                                                .child(senderRoom)
                                                .child("messages")
                                                .child(randomKey)
                                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                database.getReference().child("chats")
                                                        .child(receiverRoom)
                                                        .child("messages")
                                                        .child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
        else if(requestCode == 100)
        {
            // send multiple images
            ArrayList<String> returnValue = data.getStringArrayListExtra(Pix.IMAGE_RESULTS);
            Calendar calendar = Calendar.getInstance();
            for(int i=0; i<returnValue.size();i++)
            {
                System.out.println("image names");
                System.out.println(returnValue.get(i));
            }

            SystemClock.sleep(10000);

            for(int i=0; i<returnValue.size();i++)
            {

                StorageReference reference = storage.getReference().child("chats")
                        .child(UUID.randomUUID().toString());
                dialog.show();
                String selectedImg = returnValue.get(i);

//                Uri tempImage = data.getData();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(new File(selectedImg)));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                System.out.println("fullsizebp");

                Bitmap reducedBp = ImageResizer.reduceBitmapSize(bitmap, 2073600);
                System.out.println("reducedBp");

                File reducedFile = getBitMapFile(reducedBp);
                System.out.println("reducedfile");

                reference.putFile(Uri.fromFile(reducedFile)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
                        dialog.dismiss();
                        if(task.isSuccessful()){
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String filePath = uri.toString();
                                    String messageText = binding.messageBox.getText().toString();
                                    Date date = new Date();
                                    Message message = new Message(messageText, senderUid, date.getTime());
                                    message.setImageUrl(filePath);
                                    message.setMessage("photo");
                                    binding.messageBox.setText("");
                                    String randomKey = database.getReference().push().getKey(); //to get a uuid for pushed msg. use this same id for msg in sender and receiver data

                                    HashMap<String,Object> lastMsgObj = new HashMap<>();
                                    lastMsgObj.put("lastMsg", message.getMessage());
                                    lastMsgObj.put("lastMsgTime", date.getTime());
                                    database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                    lastMsgObj.put("read", false);  // read status to false only for receiver
                                    database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);


                                    database.getReference().child("chats")
                                            .child(senderRoom)
                                            .child("messages")
                                            .child(randomKey)
                                            .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            database.getReference().child("chats")
                                                    .child(receiverRoom)
                                                    .child("messages")
                                                    .child(randomKey)
                                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            }
        }
    }



    private File getBitMapFile(Bitmap reducedBp) {
        File file = new File(Environment.getExternalStorageDirectory()+File.separator+"reduced_file");
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        reducedBp.compress(Bitmap.CompressFormat.JPEG, 100, bo);
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


    public String getFormattedDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEEE, MMMM d, h:mm aa";
        final long HOURS = 60 * 60 * 60;
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE) ) {
            return "Today " + DateFormat.format(timeFormatString, smsTime);
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1  ){
            return "Yesterday " + DateFormat.format(timeFormatString, smsTime);
        } else if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        } else {
            return DateFormat.format("MMMM dd yyyy, h:mm aa", smsTime).toString();
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
//        String lastSeenTime = getFormattedDate(this, Calendar.getInstance().getTimeInMillis());
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence")
                .child(currentId)
                .setValue(Calendar.getInstance().getTimeInMillis()+"");
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}