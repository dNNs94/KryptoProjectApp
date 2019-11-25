package com.example.krypto2factor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.krypto2factor.Utils.VolleyCallback;

import java.util.HashMap;
import java.util.Map;


public class OTPApproverActivity extends BroadcastReceiver {

    private static final String TAG = "OTPApproverActivity";
    private static final String URL = "http://10.0.2.2:8080/verify_otp_app";

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
    public void onReceive(Context context, Intent intent) {

        Bundle intentBundle = intent.getExtras();
        if(queue == null) {
            queue = Volley.newRequestQueue(context);
        }
        String otp = intentBundle.getString("otp");
        String userID = intentBundle.getString("user_id");
        sendOtp(otp, userID, new VolleyCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Send Answer");
            }
        });

    }
}
