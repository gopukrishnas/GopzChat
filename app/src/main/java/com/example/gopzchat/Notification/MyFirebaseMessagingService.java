package com.example.gopzchat.Notification;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;

import android.os.Vibrator;
import android.util.Log;

import com.example.gopzchat.Activities.MainActivity;
import com.example.gopzchat.Activities.OpenerActivity;
import com.example.gopzchat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    NotificationManager mNotificationManager;
    FirebaseDatabase database;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        database = FirebaseDatabase.getInstance();
        database.getReference().child("token").child(FirebaseAuth.getInstance().getUid()).setValue(token);
        //Add your token in your sharepreferences.
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
    }


    //Whenewer you need FCM token, just call this static method to get it.
    public static String getToken(Context context) {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString("fcm_token", "empty");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        // playing audio and vibration when user se reques
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.setLooping(false);
        }

        // vibration
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {100, 300};
        v.vibrate(pattern, -1);


//        int resourceImage = getResources()
//                .getIdentifier(remoteMessage.getNotification()
//                        .getIcon(), "drawable", getPackageName());



        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            builder.setSmallIcon(R.drawable.icontrans);
            builder.setSmallIcon(R.drawable.ic_send);
        } else {
//            builder.setSmallIcon(R.drawable.icon_kritikar);
            builder.setSmallIcon(R.drawable.ic_send);
        }



        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        builder.setContentTitle(remoteMessage.getNotification().getTitle());
        builder.setContentText(remoteMessage.getNotification().getBody());
        builder.setContentIntent(pendingIntent);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(remoteMessage.getNotification().getBody()));
        builder.setAutoCancel(true);
        builder.setPriority(Notification.PRIORITY_MAX);

        mNotificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }



// notificationId is a unique int for each notification that you must define
        mNotificationManager.notify(100, builder.build());


    }

}

//public class MyFirebaseMessagingService extends FirebaseMessagingService {
//    public static final String FCM_PARAM = "picture";
//    private static final String CHANNEL_NAME = "FCM";
//    private static final String CHANNEL_ID = "uuid";
//    private static final String CHANNEL_DESC = "Firebase Cloud Messaging";
//    private int numMessages = 0;
//    FirebaseDatabase database;
//
//    @Override
//    public void onNewToken(String token) {
//        super.onNewToken(token);
//        database = FirebaseDatabase.getInstance();
//        database.getReference().child("token").child(FirebaseAuth.getInstance().getUid()).setValue(token);
//        //Add your token in your sharepreferences.
//        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
//    }
//
//
//    //Whenewer you need FCM token, just call this static method to get it.
//    public static String getToken(Context context) {
//        return context.getSharedPreferences("_", MODE_PRIVATE).getString("fcm_token", "empty");
//    }
//
//
//    @Override
//    public void onMessageReceived(RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//        RemoteMessage.Notification notification = remoteMessage.getNotification();
//        Map<String, String> data = remoteMessage.getData();
//        Log.d("FROM", remoteMessage.getFrom());
//        sendNotification(notification, data);
//        new jumper(getApplicationContext()).init();
//    }
//
//    private void sendNotification(RemoteMessage.Notification notification, Map<String, String> data) {
//        Bundle bundle = new Bundle();
//        bundle.putString(FCM_PARAM, data.get(FCM_PARAM));
//
//        Intent intent = new Intent(this, OpenerActivity.class);
//        intent.putExtras(bundle);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle(notification.getTitle())
//                .setContentText(notification.getBody())
//                .setAutoCancel(true)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//                //.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.win))
//                .setContentIntent(pendingIntent)
//                .setContentInfo("Hello")
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_gopz))
//                .setColor(getColor(R.color.green))
//                .setLights(Color.RED, 1000, 300)
//                .setDefaults(Notification.DEFAULT_VIBRATE)
//                .setNumber(++numMessages)
//                .setSmallIcon(R.drawable.ic_send);
//
//        try {
//            String picture = data.get(FCM_PARAM);
//            if (picture != null && !"".equals(picture)) {
//                URL url = new URL(picture);
//                Bitmap bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream());
//                notificationBuilder.setStyle(
//                        new NotificationCompat.BigPictureStyle().bigPicture(bigPicture).setSummaryText(notification.getBody())
//                );
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
//            );
//            channel.setDescription(CHANNEL_DESC);
//            channel.setShowBadge(true);
//            channel.canShowBadge();
//            channel.enableLights(true);
//            channel.setLightColor(Color.RED);
//            channel.enableVibration(true);
//            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
//
//            assert notificationManager != null;
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        assert notificationManager != null;
//        notificationManager.notify(0, notificationBuilder.build());
//    }
//}
