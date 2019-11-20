package com.example.krypto2factor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class QRScanActivity extends AppCompatActivity {
    // View Componetns
    SurfaceView mSurfaceView;
    TextView mTxtBarcode;

    // Util
    private BarcodeDetector mQRCodeDetector;
    private CameraSource mCameraSource;

    // Finals
    private static final String TAG = "QRScanActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        mSurfaceView = findViewById(R.id.surfaceView);
        mTxtBarcode = findViewById(R.id.txtBarcode);
    }

    private void initDetectorSource() {
        Toast.makeText(getApplicationContext(), "QR Code Scan started...", Toast.LENGTH_SHORT).show();

        mQRCodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        mCameraSource = new CameraSource.Builder(this, mQRCodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try{
                    if (ActivityCompat.checkSelfPermission(QRScanActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                       mCameraSource.start(mSurfaceView.getHolder());
                    }
                    else {
                        ActivityCompat.requestPermissions(QRScanActivity.this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }
                }
                catch (IOException e) {
                    Log.d(TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }
        });

        mQRCodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Toast.makeText(getApplicationContext(), "Stopping scanner to prevent memory leaks", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();

                if(qrCodes.size() != 0) {
                    mTxtBarcode.post(new Runnable() {
                        @Override
                        public void run() {
                            mTxtBarcode.removeCallbacks(null);
                            String intentData = qrCodes.valueAt(0).rawValue;
                            mTxtBarcode.setText(getString(R.string.str_found_otp, intentData));
                            Intent result = new Intent();
                            result.setData(Uri.parse(intentData));
                            setResult(RESULT_OK, result);
                            finish();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initDetectorSource();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent intent = new Intent();
        intent.setData(Uri.parse("QRScan got canceled"));
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
