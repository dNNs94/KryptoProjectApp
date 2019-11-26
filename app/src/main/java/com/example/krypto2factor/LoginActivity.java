package com.example.krypto2factor;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.krypto2factor.Utils.VolleyCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    // Finals
    private static final String TAG = "LoginActivity";
    private static final String URL_LOGIN_PW = "http://10.0.2.2:8080/authenticate_app";
    private static final String URL_QR_CODE = "http://10.0.2.2:8080/verify_otp_app";
    private static final String URL_REG_DEV = "http://10.0.2.2:8080/insert_user_device";
    private static final int QR_REQUEST_CODE = 100;
    // Volley Request queue
    RequestQueue queue;
    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private ProgressBar mProgressView;
    private View mLoginFormView;
    private Button mEmailSignInButton;
    private Button mQRCodeButton;
    // Util
    private String mDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up login form
        mEmailView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);

        // Setup login buttons
        mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mQRCodeButton = findViewById(R.id.btn_qr);

        // Bind login method to click
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        // Bind QR code scan to click
        mQRCodeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(LoginActivity.this, QRScanActivity.class), QR_REQUEST_CODE);
            }
        });

        // Bind login method to confirm task on system keyboard
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);


        // Create Request Queue
        queue = Volley.newRequestQueue(this);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        mDeviceId = task.getResult().getToken();
                    }
                });

    }

    /**
     * Checks if QR-Code Scan was successful and registers device in backend if so
     * @param requestCode code to identify which request data is awaited
     * @param resultCode code to define whether data was received successfully or not
     * @param data extras put into the intent to receive here (userId and otp)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == QR_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String receivedData = data.getDataString();
                if(receivedData != null && !receivedData.equals("")) {
                    try{
                        JSONObject jsonObject = new JSONObject(data.getDataString());

                        String receivedOtp = jsonObject.getString("otp");
                        String receivedUserId = jsonObject.getString("user_id");

                        requestLoginWithQR(receivedUserId, receivedOtp, new VolleyCallback() {
                            @Override
                            public void onSuccess(String result) {
                                if(result.contains("Verification valid")) {
                                    requestDeviceRegistration(new VolleyCallback() {
                                        @Override
                                        public void onSuccess(String result) {
                                            if(result.contains("Successfully inserted device")) {
                                                Toast.makeText(getApplicationContext(), "Device registration complete!", Toast.LENGTH_SHORT).show();
                                                getOtpIntent();
                                            }
                                            else if(result.contains("Device is already active")) {
                                                Toast.makeText(getApplicationContext(), "Logged in with active device!", Toast.LENGTH_SHORT).show();
                                                getOtpIntent();
                                            }
                                            else {
                                                Toast.makeText(getApplicationContext(), "Error during device registration!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                }
                                else {
                                    mEmailView.setError("OTP Rejected");
                                    mEmailView.requestFocus();
                                }
                            }
                        });
                    }
                    catch (Exception e){
                        mEmailView.setError("Something went wrong");
                        mEmailView.requestFocus();

                        Log.d(TAG, e.getMessage(), e);
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    /**
     * Attempts to sign in to the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();

        // Reset locals | Track focusView and cancellation switch
        boolean cancel = false;
        View focusView = null;

        // Check for a non-empty password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid non-empty email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            requestLoginWithPW(email, password, new VolleyCallback() {
                @Override
                public void onSuccess(String result) {
                    try {
                        Log.d(TAG, "RESPONSE: " + result);
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            // Get object parameter-values into variables
                            int status = jsonObject.getInt("status");
                            String message = jsonObject.getString("message");
                            // Switch through status code to determine further action
                            switch (status) {
                                // 200 - Success! Continue
                                case 200:
                                    Intent otpIntent = getOtpIntent();
                                    otpIntent.putExtra("deviceId", mDeviceId);
                                    startActivity(otpIntent);
                                    break;
                                // 403 - Forbidden! Display error message
                                case 403:
                                // 404 - Not found! Display error message
                                case 404:
                                    mEmailView.setError(message);
                                    mEmailView.requestFocus();
                                    break;
                            }
                        } catch (JSONException err) {
                            Log.d(TAG, err.toString());
                        }

                    } catch (Exception e) {
                        // Log exceptions to debug
                        Log.d(TAG, e.getMessage(), e);
                        e.printStackTrace();
                        // Notify user something went wrong
                        mEmailView.setError("A Server Error Occured!");
                        mEmailView.requestFocus();
                    }
                }
            });
        }
    }

    /**
     * Check entered Email against regular expression to have at least one
     * "@"-symbol between to letter-pairs
     * @param email users email address (Value from {@link LoginActivity#mEmailView})
     */
    private boolean isEmailValid(String email) {
        String regex = "^(.+)@(.+)$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(email);

        return m.matches();
    }

    /**
     * Switch to OTPActivity via Intent
     */
    private Intent getOtpIntent(){
        return new Intent(this, OTPActivity.class);
    }

    /**
     * Volley request to login to the app via py backend (used in {@link LoginActivity#attemptLogin()})
     * @param email users email address
     * @param password users password
     * @param callback callback method to handle result via Interface (see: {@link com.example.krypto2factor.Utils.VolleyCallback})
     */
    private void requestLoginWithPW(final String email, final String password, final VolleyCallback callback) {
        Response.Listener<String> responseListener = new Response.Listener<String>() {
            // Catch the Response
            @Override
            public void onResponse(String response) {
                callback.onSuccess(response);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // Log exceptions to debug
                Log.d(TAG, error.getMessage(), error);
                error.printStackTrace();

                // Notify user something went wrong
                mEmailView.setError("A Server Error Occured!");
                mEmailView.requestFocus();
            }
        };

        StringRequest req = new StringRequest(Request.Method.POST, URL_LOGIN_PW, responseListener, errorListener) {
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> mParams = new HashMap<String, String>();
                mParams.put("email", email);
                mParams.put("password", password);
                mParams.put("device_id", mDeviceId);
                mParams.put("device_name", android.os.Build.MODEL);
                return mParams;
            }
        };

        queue.add(req);
    }

    /**
     * Volley request to login to the app via py backend using QR-Scan
     * @param userId userId from QR ({@link QRScanActivity})
     * @param otp users otp from QR ({@link QRScanActivity})
     * @param callback callback method to handle result via Interface (see: {@link com.example.krypto2factor.Utils.VolleyCallback})
     */
    private void requestLoginWithQR(final String userId, final String otp, final VolleyCallback callback) {
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

                mEmailView.setError("Error while resolving QR-Code!");
                mEmailView.requestFocus();
            }
        };

        StringRequest req = new StringRequest(Request.Method.POST, URL_QR_CODE, responseListener, errorListener) {
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> mParams = new HashMap<String, String>();
                mParams.put("otp", otp);
                mParams.put("user_id", userId);
                return mParams;
            }
        };

        queue.add(req);
    }

    /**
     * Volley request to register this device in database via py backend
     * @param callback callback method to handle result via Interface (see: {@link com.example.krypto2factor.Utils.VolleyCallback})
     */
    private void requestDeviceRegistration(final VolleyCallback callback) {
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

                mEmailView.setError("Error while registering Device!");
                mEmailView.requestFocus();
            }
        };

        StringRequest req = new StringRequest(Request.Method.POST, URL_REG_DEV, responseListener, errorListener) {
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> mParams = new HashMap<String, String>();
                mParams.put("device_id", mDeviceId);
                mParams.put("device_name", Build.MODEL);
                return mParams;
            }
        };

        queue.add(req);
    }

    /**
     * Animate progressbar while receiving a result from the server
     */
    public class ProgressBarAnimation extends Animation {
        private ProgressBar progressBar;
        private float from;
        private float to;

        ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
            super();
            this.progressBar = progressBar;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int) value);
        }
    }
}