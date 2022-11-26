package com.borisbordeaux.arsudokusolver.classifier;

import android.content.Context;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;

import com.borisbordeaux.arsudokusolver.utils.Utils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class TensorFlowNumberClassifier implements INumberClassifier {

    //tag for debug logs
    private final String TAG = "OCR";

    //context to get file directory
    private final Context context;
    private final String saveDir;
    private final boolean saveImage;
    private final Size size;
    //resized mat for the inference
    //avoid a new each time, so we
    //gain performance
    Mat resized = new Mat();
    private int counter;
    //opencv dnn net
    private Net net;

    /**
     * Constructor
     *
     * @param context the context to get file directory
     */
    public TensorFlowNumberClassifier(Context context) {
        this.context = context;
        this.counter = 0;
        this.saveDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/images/";
        this.saveImage = false;

        size = new Size(28, 28);
    }

    @Override
    public int getNumber(Mat img) {
        //img is a 104x104x1 Mat
        //resize the image to a 28x28x1 Mat
        Imgproc.resize(img, resized, size);

        Log.d(TAG, "Size of image is : " + img.size().width + "/" + img.size().height);

        //use the mask to set borders to 0
        //Core.subtract(resized, mask, resized);

        Imgproc.threshold(img, img, 10, 255, Imgproc.THRESH_BINARY);

        //save input image
        if (saveImage) {
            Utils.saveMat(resized, saveDir, counter + ".png");
            counter++;
            counter %= 81;
        }

        //convert to a float image
        resized.convertTo(resized, CvType.CV_32F, 1.0 / 255.0, 0);

        //create a Binary Large Object based on the image
        Mat blob = Dnn.blobFromImage(resized);
        //set blob as input
        net.setInput(blob);
        //make the inference
        Mat result = net.forward();
        //release the memory of the blob since it will not be used after
        blob.release();
        //reshape the resulting blob to get a Mat with 10 elements
        result.reshape(10);

        Log.d(TAG, "size of the result " + result.size().toString() + " of type " + result.type());
        int res = softMax(result);

        //free the memory of the result
        result.release();

        return res;
    }

    /**
     * Loads assets for the model
     *
     * @return true if the assets were loaded correctly, false otherwise
     */
    public boolean loadAssets() {
        boolean loaded = false;

        String dataPath = "";
        try {
            InputStream is = context.getAssets().open("frozen_graph.pb");
            File dir = new File(context.getFilesDir().getAbsolutePath() + "/graph/");
            if (!dir.exists()) {
                if (dir.mkdir()) {
                    Log.d(TAG, "Directory created !");
                } else {
                    Log.d(TAG, "Directory not created !");
                }
            } else {
                Log.d(TAG, "Directory already exists !");
            }
            Log.d(TAG, "directory is " + dir.getAbsolutePath());

            File f = new File(context.getFilesDir().getAbsolutePath() + "/graph/frozen_graph.pb");
            if (f.createNewFile()) {
                Log.d(TAG, "File created !");
                OutputStream os = new FileOutputStream(f);

                FileUtils.copy(is, os);
                Log.d(TAG, "File copied !");
            } else {
                Log.d(TAG, "File already exists");
            }
            dataPath = f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "data path is " + dataPath);

        if (!"".equals(dataPath)) {
            net = Dnn.readNetFromTensorflow(dataPath);
            loaded = true;
        }

        return loaded;
    }

    /**
     * Does a softmax on the given array
     *
     * @param result the Mat containing inference results
     * @return the index where the value is the higher in the data array
     */
    private int softMax(Mat result) {
        float[] data = new float[10];
        result.get(0, 0, data);

        Log.d(TAG, "the result " + Arrays.toString(data));

        int val = 0;
        float max = data[0];
        for (int i = 1; i < 10; i++) {
            if (data[i] > max) {
                val = i;
                max = data[i];
            }
        }

        Log.d(TAG, "the number found is " + val + " with confidence " + max);

        //if confidence is less than 99%, set to 0
        //avoid unsure and false values
        if (max < 0.99)
            val = 0;

        return val;
    }
}
