package com.example.krypto2factor;

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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
    private static final String URL = "http://10.0.2.2:8080/authenticate_app";
    // Volley Request queue
    RequestQueue queue;
    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private ProgressBar mProgressView;
    private View mLoginFormView;
    // Util
    private String mDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up login form
        mEmailView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);

        // Setup login button
        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);

        // Bind login method to click
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
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
            // Animate progressbar ToDo: Make visible, adjust layout
            ProgressBarAnimation animation = new ProgressBarAnimation(mProgressView, 0, 100);
            animation.setDuration(500);
            animation.start();

            // Create Request Queue
            queue = Volley.newRequestQueue(this);

            // Create a JSON Object Request
            StringRequest req = new StringRequest
                    (Request.Method.POST, URL, new Response.Listener<String>() {

                        // Catch the Response
                        @Override
                        public void onResponse(String response) {
                            try {
                                Log.d(TAG, "RESPONSE: " + response);
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    // Get object parameter-values into variables
                                    int status = jsonObject.getInt("status");
                                    String message = jsonObject.getString("message");
                                    int data = jsonObject.getInt("data");
                                    // Switch through status code to determine further action
                                    switch (status) {
                                        // 200 - Success! Continue
                                        case 200:
                                            // ToDo: Continue to request_otp + intent to otp activity
                                            Log.d(TAG, "Data received: " + data);
                                            mEmailView.setError(message);
                                            mEmailView.requestFocus();
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
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // Log exceptions to debug
                            Log.d(TAG, error.getMessage(), error);
                            error.printStackTrace();

                            // Notify user something went wrong
                            mEmailView.setError("A Server Error Occured!");
                            mEmailView.requestFocus();
                        }
                    }) {
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
    }

    /**
     * Check entered Email against regular expression to have at least one
     * "@"-symbol between to letter-pairs
     */
    private boolean isEmailValid(String email) {
        String regex = "^(.+)@(.+)$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(email);

        return m.matches();
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