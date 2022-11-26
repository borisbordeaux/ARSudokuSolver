package com.borisbordeaux.arsudokusolver.classifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.FileUtils;
import android.util.Log;

import com.borisbordeaux.arsudokusolver.utils.Utils;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TesseractNumberClassifier implements INumberClassifier {

    //TAG for debug logs
    private final static String TAG = "OCR";

    //tesseract base api
    private final TessBaseAPI tesseractAPI;

    //the context to get file directories
    private final Context context;

    //Region Of Interest used to crop the arriving image
    private Rect ROI;

    /**
     * Constructor
     *
     * @param context to get file directory
     */
    public TesseractNumberClassifier(Context context) {
        this.context = context;
        tesseractAPI = new TessBaseAPI();
    }


    @Override
    public int getNumber(Mat img) {
        //img is a 104x104x1 Mat
        //we have to crop the image
        int cropValue = 14;

        //create the ROI if it is null
        if (ROI == null)
            ROI = new Rect(cropValue, cropValue, img.cols() - cropValue * 2, img.rows() - cropValue * 2);

        //create a Mat by copy only the
        Mat resized = new Mat(img, ROI);

        Log.d(TAG, "Size of image is : " + img.size().width + "/" + img.size().height);

        //we apply a blur and threshold to remove the noise
        Imgproc.GaussianBlur(resized, resized, new Size(3, 3), 0, 0, Core.BORDER_REPLICATE);
        Imgproc.threshold(resized, resized, 20, 255, Imgproc.THRESH_BINARY_INV);

        //we create a bitmap image
        Bitmap bmp = Utils.MatToBitmap(resized);

        //use the tesseract api to infer ont the bitmap image
        tesseractAPI.setImage(bmp);
        String text = tesseractAPI.getUTF8Text();
        int confidence = tesseractAPI.meanConfidence();

        Log.d(TAG, "value : " + text + " at " + confidence + "%");

        //release the memory
        resized.release();

        //return 0 if not enough confidence, if text is not found or if text found is a space
        //return the integer corresponding to the text found
        return ("".equals(text) || confidence < 80 || " ".equals(text)) ? 0 : Integer.parseInt(text);
    }

    /**
     * Load assets
     *
     * @return true if the assets are correctly loaded
     */
    public boolean loadAssets() {
        boolean loaded = false;
        String dataPath = "";
        try {
            InputStream is = context.getAssets().open("dataocr");
            File dir = new File(context.getFilesDir().getAbsolutePath() + "/tessdata/");
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

            File f = new File(context.getFilesDir().getAbsolutePath() + "/tessdata/eng.traineddata");
            if (f.createNewFile()) {
                Log.d(TAG, "File created !");
                OutputStream os = new FileOutputStream(f);

                FileUtils.copy(is, os);
                Log.d(TAG, "File copied !");
            } else {
                Log.d(TAG, "File already exists");
            }
            dataPath = context.getFilesDir().getAbsolutePath() + "/";
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "data path is " + dataPath);

        if (!"".equals(dataPath)) {
            tesseractAPI.init(dataPath, "eng", TessBaseAPI.OEM_DEFAULT);
            tesseractAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
            tesseractAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789 ");
            loaded = true;
        }

        return loaded;
    }
}
