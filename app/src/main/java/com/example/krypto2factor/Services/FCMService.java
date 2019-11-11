package com.example.krypto2factor.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.example.krypto2factor.NotificationID;
import com.example.krypto2factor.OTPApproverActivity;
import com.example.krypto2factor.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import androidx.core.app.NotificationCompat;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;


public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    public FCMService() {

    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        // ToDo: sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            /*ToDo: if (/* Check if data needs to be processed by long running job == true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                ToDo: scheduleJob();
            } else {
                // Handle message within 10 seconds
                ToDo: handleNow();
            }*/

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            String body = remoteMessage.getNotification().getBody();
            String title = remoteMessage.getNotification().getTitle();
            Log.d(TAG, "Message Notification Body: " + body);
            try {
                showNotification(title, body);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void showNotification(String title, String body) throws JSONException {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "com.example.krypto2factor.test";
        int UNIQUE_INT_PER_CALL = NotificationID.getID();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification", NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.setDescription("krypto2factor Channel test");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationManager.createNotificationChannel(notificationChannel);
        }

        JSONObject jsonObject = new JSONObject(body);
        Intent approveIntent = new Intent(this, OTPApproverActivity.class);
        approveIntent.putExtra("otp", jsonObject.getString("otp"));
        approveIntent.putExtra("user_id", jsonObject.getString("user_id"));
        approveIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent approvePendingIntent =
                PendingIntent.getBroadcast(this, UNIQUE_INT_PER_CALL, approveIntent, FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("Dein OTP ist bereit: " + jsonObject.getString("otp"))
                .setContentInfo("Info")
                .setContentIntent(approvePendingIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_dark_normal, "Approve",
                        approvePendingIntent);

        notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
    }
}
