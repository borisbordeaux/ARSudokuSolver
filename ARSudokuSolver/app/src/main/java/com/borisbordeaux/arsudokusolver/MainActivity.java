package com.borisbordeaux.arsudokusolver;

import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.borisbordeaux.arsudokusolver.utils.log.ConsoleLogger;
import com.borisbordeaux.arsudokusolver.utils.log.ILogger;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;

import java.util.concurrent.ExecutionException;

import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class MainActivity extends AppCompatActivity {

    //the tag for debug logs
    private static final String TAG = "MainActivityOpenCV";
    private static final ILogger mLogger = new ConsoleLogger();

    //load opencv statically
    static {
        if (OpenCVLoader.initDebug(true)) {
            mLogger.log(TAG, "OpenCV is configured or connected successfully");
        } else {
            mLogger.log(TAG, "OpenCV not working or loaded");
        }
    }

    //all required permissions (here only camera)
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    //the image analyzer to analyse the image in preview
    private ImageAnalyzer mAnalyzer;

    //preview of the camera
    private GPUImageView mPreviewView;

    //button to scan the image in preview
    private Button mButtonScan;

    //the camera
    private Camera mCamera;

    //the switch to switch on or switch off the torch
    private SwitchCompat mTorchSwitch;

    /**
     * {@inheritDoc}
     * Called 1 time when app opens, init the app
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
     * {@inheritDoc}
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

                //build the imageAnalysis
                ImageAnalysis imageAnalysis = builder
                        //set the resolution of the view
                        .setTargetResolution(new android.util.Size(400, 400))
                        //the executor receives the last available frame from the camera at the time that the analyze() method is called
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                //sets the analyzer
                imageAnalysis.setAnalyzer(getMainExecutor(), mAnalyzer);

                // Attach use cases to the camera with the same lifecycle owner
                mCamera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);

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
     * Sets listeners for events handling on scan button (touch)
     * and preview view (long touch)
     */
    public void setListeners() {
        //scan the image on click
        mButtonScan.setOnClickListener(view -> mAnalyzer.rescan());

        //change between display intermediate output
        //and processed image output with results
        mPreviewView.setOnLongClickListener(view -> {
            mAnalyzer.toggleDisplayIntermediate();
            return true;
        });

        mTorchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mCamera.getCameraControl().enableTorch(isChecked));
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
        } else {
            t.setText(R.string.error);
        }

        t.show();
    }

    /**
     * Init the app (widgets, analyzer, camera and tensorflow model)
     */
    private void init() {
        //get the widgets in the view
        mPreviewView = findViewById(R.id.previewView);
        mButtonScan = findViewById(R.id.analyze);
        mTorchSwitch = findViewById(R.id.torch_switch);

        mAnalyzer = new ImageAnalyzer(mPreviewView);
        startCamera();
        loadTensorflow();
    }
}