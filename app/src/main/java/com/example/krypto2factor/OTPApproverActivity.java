package com.example.krypto2factor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.krypto2factor.Utils.VolleyCallback;

import java.util.HashMap;
import java.util.Map;

import static com.example.krypto2factor.Utils.CertificateManager.getHurlStack;


public class OTPApproverActivity extends BroadcastReceiver {

    private static final String TAG = "OTPApproverActivity";
    private static final String URL = "https://172.50.1.12:443/verify_otp_app"; // https://9e01f831.ngrok.io/ http://10.0.2.2

    RequestQueue queue;

    private void sendOtp(final String otp, final String userID, final VolleyCallback callback) {
        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                callback.onSuccess(response);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.getMessage(), error);
                error.printStackTrace();
            }
        };


        StringRequest otpReq = new StringRequest(Request.Method.POST, URL, responseListener, errorListener){
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("otp", otp);
                params.put("user_id", userID);
                return params;
            }
        };

        queue.add(otpReq);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Bundle intentBundle = intent.getExtras();
        if(queue == null) {
            HurlStack hurlStack = getHurlStack(context);
            queue = Volley.newRequestQueue(context, hurlStack);
        }
        String otp = intentBundle.getString("otp");
        String userID = intentBundle.getString("user_id");
        final int notificationId = intentBundle.getInt("notificationId");
        sendOtp(otp, userID, new VolleyCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Send Answer");
                NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if(nManager != null)
                    nManager.cancel(notificationId);
            }
        });

    }
}
