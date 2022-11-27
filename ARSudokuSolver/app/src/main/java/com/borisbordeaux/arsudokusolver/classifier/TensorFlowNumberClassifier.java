package com.borisbordeaux.arsudokusolver.classifier;

import android.content.Context;
import android.os.FileUtils;

import com.borisbordeaux.arsudokusolver.utils.log.AndroidLogger;
import com.borisbordeaux.arsudokusolver.utils.log.ILogger;

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
    private final ILogger mLogger = new AndroidLogger();

    //context to get file directory
    private final Context mContext;

    private final Size size = new Size(28, 28);

    //resized mat for the inference
    //avoid a new each time, so we
    //gain performance
    Mat resized = new Mat();

    //opencv dnn net
    private Net net = null;

    /**
     * Constructor, needs to call load assets to use
     *
     * @param context the context to get file directory
     */
    public TensorFlowNumberClassifier(Context context) {
        this.mContext = context;
    }

    /**
     * {@inheritDoc}
     *
     * @param img the image that has to be classified
     * @return the number detected in the image or 0 if the net was not loaded
     */
    @Override
    public int getNumber(Mat img) {
        if (net != null) {
            //resize the image to a 28x28x1 Mat
            Imgproc.resize(img, resized, size);

            Imgproc.threshold(img, img, 10, 255, Imgproc.THRESH_BINARY);

            //convert to a float image
            resized.convertTo(resized, CvType.CV_32F, 1.0 / 255.0, 0);

            Mat blob = Dnn.blobFromImage(resized);
            net.setInput(blob);
            Mat result = net.forward();

            //free the memory of the blob
            blob.release();

            //reshape the resulting blob to get a Mat with 10 elements
            result.reshape(10);

            int res = softMax(result);

            //free the memory of the result
            result.release();

            return res;
        } else {
            return 0;
        }
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
            InputStream is = mContext.getAssets().open("frozen_graph.pb");
            File dir = new File(mContext.getFilesDir().getAbsolutePath() + "/graph/");
            if (!dir.exists()) {
                if (dir.mkdir()) {
                    mLogger.log(TAG, "Directory created !");
                } else {
                    mLogger.log(TAG, "Directory not created !");
                }
            } else {
                mLogger.log(TAG, "Directory already exists !");
            }
            mLogger.log(TAG, "directory is " + dir.getAbsolutePath());

            File f = new File(mContext.getFilesDir().getAbsolutePath() + "/graph/frozen_graph.pb");
            if (f.createNewFile()) {
                mLogger.log(TAG, "File created !");
                OutputStream os = new FileOutputStream(f);

                FileUtils.copy(is, os);
                mLogger.log(TAG, "File copied !");
            } else {
                mLogger.log(TAG, "File already exists");
            }
            dataPath = f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mLogger.log(TAG, "data path is " + dataPath);

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

        mLogger.log(TAG, "the result " + Arrays.toString(data));

        int val = 0;
        float max = data[0];
        for (int i = 1; i < 10; i++) {
            if (data[i] > max) {
                val = i;
                max = data[i];
            }
        }

        mLogger.log(TAG, "the number found is " + val + " with confidence " + max);

        //if confidence is less than 99%, set to 0
        //avoid unsure and false values
        if (max < 0.99)
            val = 0;

        return val;
    }
}
