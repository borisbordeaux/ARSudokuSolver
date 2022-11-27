package com.borisbordeaux.arsudokusolver.analyzer;

import android.graphics.Bitmap;

import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageProxy;

import com.borisbordeaux.arsudokusolver.classifier.INumberClassifier;
import com.borisbordeaux.arsudokusolver.utils.image.ImageConverter;
import com.borisbordeaux.arsudokusolver.utils.log.AndroidLogger;
import com.borisbordeaux.arsudokusolver.utils.log.ILogger;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class ImageAnalyzer implements Analyzer {

    private static final String TAG = "Analyzer";
    private final ILogger mLogger = new AndroidLogger();
    private final Size SQUARE_SIZE = new Size(480, 480);
    private final GPUImageView mPreviewView;
    private final ImageProcessor mImageProcessor = new ImageProcessor();
    private final Mat mOutput = new Mat();
    private final Mat mRGB = new Mat();
    private Bitmap mBmp = null;
    private boolean mDisplayIntermediate = false;

    public ImageAnalyzer(GPUImageView view) {
        this.mPreviewView = view;
    }

    @Override
    public void analyze(@NotNull ImageProxy image) {

        //convert image Yuv to Mat RGB
        processInput(image);

        if (mImageProcessor.getNumberClassifier() != null) {
            if (mDisplayIntermediate) {
                mImageProcessor.getIntermediate(mRGB, mOutput);
            } else {
                mImageProcessor.getFinalImage(mRGB, mOutput);
            }
        } else {
            mRGB.copyTo(mOutput);
        }

        processOutput();

        image.close();
    }

    public void invertDisplayIntermediate() {
        mDisplayIntermediate = !mDisplayIntermediate;
    }

    public void rescan() {
        mImageProcessor.reScan();
    }

    public void setNumberClassifier(INumberClassifier classifier) {
        mImageProcessor.setNumberClassifier(classifier);
    }

    private void processInput(@NotNull ImageProxy image) {
        ImageConverter.convYUV2RGB(image, mRGB);

        Core.rotate(mRGB, mRGB, Core.ROTATE_90_CLOCKWISE);
        Imgproc.resize(mRGB, mRGB, SQUARE_SIZE);

        mLogger.log(TAG, "new image size : " + mRGB.size().toString());
    }

    private void processOutput() {
        if (mBmp == null) {
            mBmp = Bitmap.createBitmap(mOutput.cols(), mOutput.rows(), Bitmap.Config.ARGB_8888);
        }
        ImageConverter.MatToBitmap(mOutput, mBmp);

        mPreviewView.post(() -> mPreviewView.setImage(mBmp));
    }

}
