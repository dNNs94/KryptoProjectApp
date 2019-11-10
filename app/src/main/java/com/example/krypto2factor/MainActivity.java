package com.example.krypto2factor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    /*
    *   ToDo: #1 Look into FCM for Push Notifications -> moved to later progress
    *   ToDo: #2 Setup DB Locally - Check
    *   ToDo: #3 Use Localhost Address: http://10.0.2.2 - Check
    *   ToDo: #4 Get Data from Server Somehow - Check
    *   ToDo: #5 Setup initial Device Registration - PoC Check | ToDo: Look into authentication via scanning QR to deliver alternative auth method -> see #5.1
    *   ToDo: #6 Make OTP be toggled via Server-Push
    *   ToDo: #7 Send Push Notification with Current OTP to User and display it in app once opened -> moved to later see #1
    *   ToDo: #8 Refactor App into Module-like Structure
    *   ToDo: #9 Optimize Styling and Layouting
    *   ToDo: #10 Encrypt Communication
    *
    *   ToDo: #5.1
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
