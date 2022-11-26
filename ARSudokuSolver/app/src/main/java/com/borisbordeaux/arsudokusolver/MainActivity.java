package com.borisbordeaux.arsudokusolver;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.borisbordeaux.arsudokusolver.analyzer.ImageAnalyzer;
import com.borisbordeaux.arsudokusolver.classifier.TensorFlowNumberClassifier;
import com.borisbordeaux.arsudokusolver.classifier.TesseractNumberClassifier;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;

import java.util.concurrent.ExecutionException;

import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class MainActivity extends AppCompatActivity {

    //the tag for debug logs
    private static final String TAG = "MainActivity";

    //load opencv statically
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV is configured or connected successfully");
        } else {
            Log.d(TAG, "OpenCV not working or loaded");
        }
    }

    //all required permissions (here only camera)
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    //the image analyzer to analyse the image in preview
    ImageAnalyzer mAnalyzer;
    //preview of the camera
    private GPUImageView previewView;
    //button to change the method used
    private Button bChange;
    //button to scan the image in preview
    private Button bScan;
    //the camera
    private Camera camera;
    //the switch to switch on or switch off the torch
    private SwitchCompat torchSwitch;
    //the current classifier
    private Classifier classifier = Classifier.NONE;

    /**
     * Called 1 time when app open, init the app
     *
     * @param savedInstanceState last instance state saved
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the layout of the main activity
        setContentView(R.layout.activity_main);

        //continue initialization if all permissions granted
        if (allPermissionsGranted()) {
            init();
        } else {
            //else request permissions and when permission request
            //ended, onRequestPermissionsResult will be called
            int REQUEST_CODE_PERMISSIONS = 101;
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * Callback when user made his choice for requested permissions
     *
     * @param requestCode  the code used when asked for permissions
     * @param permissions  the permissions the user accepted or not
     * @param grantResults the choice of the user for each permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //if all permissions are granted
        if (allPermissionsGranted()) {
            //init the app
            init();
        } else {
            //permissions not granted, the app can't run
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            //end the app
            finish();
        }
    }

    /**
     * Checks if all permissions are granted
     *
     * @return true if all permissions are granted, false otherwise
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts the camera preview
     */
    @SuppressLint("UnsafeOptInUsageError")
    public void startCamera() {
        //create the potential (future) camera provider
        //it will contains the camera provider when the
        //camera will be available
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        //define what to do when the camera provider will be available
        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                //Images are processed by passing an executor in which the image analysis is run
                ImageAnalysis.Builder builder = new ImageAnalysis.Builder();

                ImageAnalysis imageAnalysis = builder
                        //set the resolution of the view
                        .setTargetResolution(new android.util.Size(400, 400))
                        //the executor receives the last available frame from the camera at the time that the analyze() method is called
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                //sets the analyzer
                imageAnalysis.setAnalyzer(getMainExecutor(), mAnalyzer);

                // Attach use cases to the camera with the same lifecycle owner
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);

                setListeners();

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to
                // handle InterruptedException.
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Sets listeners for events handling
     */
    public void setListeners() {
        //change the method used on click
        bChange.setOnClickListener(view -> {
            if (classifier.equals(Classifier.TENSORFLOW)) {
                loadTesseract();
            } else {
                loadTensorflow();
            }
        });

        //scan the image on click
        bScan.setOnClickListener(view -> mAnalyzer.rescan());

        //change between display intermediate output
        //and processed image output with results
        previewView.setOnLongClickListener(view -> {
            mAnalyzer.invertDisplayIntermediate();
            return true;
        });

        torchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> camera.getCameraControl().enableTorch(isChecked));
    }

    /**
     * Loads the Tesseract classifier
     */
    private void loadTesseract() {
        TesseractNumberClassifier nc = new TesseractNumberClassifier(getBaseContext());

        Toast t = Toast.makeText(getBaseContext(), "", Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);

        if (nc.loadAssets()) {
            mAnalyzer.setNumberClassifier(nc);
            t.setText(R.string.tesseract);
            classifier = Classifier.TESSERACT;
        } else {
            t.setText(R.string.error);
            classifier = Classifier.NONE;
        }

        t.show();
    }

    /**
     * Loads the tensorflow model classifier
     */
    private void loadTensorflow() {
        TensorFlowNumberClassifier nc = new TensorFlowNumberClassifier(getBaseContext());

        Toast t = Toast.makeText(getBaseContext(), "", Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);

        if (nc.loadAssets()) {
            mAnalyzer.setNumberClassifier(nc);
            t.setText(R.string.tensorflow);
            classifier = Classifier.TENSORFLOW;
        } else {
            t.setText(R.string.error);
            classifier = Classifier.NONE;
        }

        t.show();
    }

    private void init() {
        //get the widgets in the view
        previewView = findViewById(R.id.previewView);
        bChange = findViewById(R.id.change_method);
        bScan = findViewById(R.id.analyze);
        torchSwitch = findViewById(R.id.torch_switch);

        //create the image analyzer
        mAnalyzer = new ImageAnalyzer(previewView);

        //start the camera
        startCamera();

        //load tensorflow model by default
        loadTensorflow();
    }

    //enum for classifiers
    private enum Classifier {
        TESSERACT, //ocr classifier
        TENSORFLOW, //cnn classifier
        NONE //when no other classifier loaded
    }
}