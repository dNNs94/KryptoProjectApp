package com.example.krypto2factor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.krypto2factor.Utils.CircularProgressBar;
import com.example.krypto2factor.Utils.VolleyCallback;

import java.util.HashMap;
import java.util.Map;

public class OTPActivity extends AppCompatActivity {

    // UI Components
    private TextView mOtpText;
    private TextView mCountdownText;
    private CircularProgressBar mProgressBar;
    // Volley Request queue
    RequestQueue queue;
    // Timer and switch to keep track of it
    CountDownTimer timer;
    boolean isRequestIntervalStopped = false;
    // Finals
    private static final String TAG = "OTPActivity";
    private static final String URL = "http://10.0.2.2:8080/request_otp_app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        /*if (savedInstanceState != null) {
            // ToDo: restore timer
        }
        else{*/
            Intent fromLoginIntent = getIntent();
            final String deviceId = fromLoginIntent.getStringExtra("deviceId");
            Log.d(TAG, "Received intent extra: " + deviceId);
        //}

        if(queue == null) {
            queue = Volley.newRequestQueue(this);
        }

        mOtpText = findViewById(R.id.txt_otp);
        mCountdownText = findViewById(R.id.txt_countdown);
        mProgressBar = findViewById(R.id.pb_refresh);
        final int otpLifetime = 60000;

        requestOtp(deviceId, new VolleyCallback() {
            @Override
            public void onSuccess(String result) {
                mOtpText.setText(getString(R.string.str_otp, result));
                timer = new CountDownTimer(otpLifetime, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long secondsLeft = (millisUntilFinished / 1000);
                        String displayText = getString(R.string.str_otp_countdown, String.valueOf(secondsLeft));
                        mCountdownText.setText(displayText);
                    }

                    @Override
                    public void onFinish() {
                        mCountdownText.setText(getString(R.string.str_req_new_otp));
                    }
                }.start();

                triggerOtpRequestInterval(otpLifetime, deviceId);
                mProgressBar.setAnimatedProgress(100, otpLifetime);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ToDo: Pause timer and save its state;
        new Handler().removeCallbacksAndMessages(null);
        isRequestIntervalStopped = true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // ToDo: Resume timer for request
        isRequestIntervalStopped = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // ToDo: Save state of the timer;
        isRequestIntervalStopped = true;
    }

    /**
     * Requests a new OTP from python backend
     * @param deviceId unique identifier of the device (FCMID)
     * @param callback callback function to handle the result
     */
    private void requestOtp(final String deviceId, final VolleyCallback callback) {
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
                params.put("device_id", deviceId);
                return params;
            }
        };

        queue.add(otpReq);
    }

    /**
     * Triggers new otp request in set interval, runtime handled in lifeclycle methods
     * @param delay time until next otp will be requested
     * @param deviceId unique identifier of the device (FCMID)
     */
    private void triggerOtpRequestInterval(final int delay, final String deviceId) {
        if(isRequestIntervalStopped) {
            new Handler().removeCallbacksAndMessages(null);
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestOtp(deviceId, new VolleyCallback() {
                    @Override
                    public void onSuccess(String result) {
                        mOtpText.setText(getString(R.string.str_otp, result));

                        timer = new CountDownTimer(delay, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                long secondsLeft = (millisUntilFinished / 1000);
                                String displayText = getString(R.string.str_otp_countdown, String.valueOf(secondsLeft));
                                mCountdownText.setText(displayText);
                            }

                            @Override
                            public void onFinish() {
                                mCountdownText.setText(getString(R.string.str_req_new_otp));
                            }
                        }.start();

                        triggerOtpRequestInterval(delay, deviceId);
                        mProgressBar.setAnimatedProgress(100, delay);
                    }
                });
            }
        }, delay);
    }
}
