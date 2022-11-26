package com.borisbordeaux.arsudokusolver.analyzer;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageProxy;

import com.borisbordeaux.arsudokusolver.classifier.INumberClassifier;
import com.borisbordeaux.arsudokusolver.utils.Utils;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class ImageAnalyzer implements Analyzer {

    private static final String TAG = "Analyzer";
    private final Size squared;
    private final GPUImageView previewView;
    private final ImageProcessor imageProcessor;
    private final Mat output;
    private byte[] nv21 = null;
    private Mat mYuv = null;
    private Mat mRGB = null;
    private Bitmap bmp = null;
    private boolean displayIntermediate = false;

    public ImageAnalyzer(GPUImageView view) {
        this.previewView = view;
        imageProcessor = new ImageProcessor();
        output = new Mat();
        squared = new Size(480, 480);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

        //convert image Yuv to Mat RGB
        processInput(image);

        //Utils.saveMat(mRGB, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "input.jpg");

        if (imageProcessor.getNumberClassifier() != null) {
            if (displayIntermediate) {
                imageProcessor.getIntermediate(mRGB, output);
            } else {
                imageProcessor.getFinalImage(mRGB, output);
            }
        } else {
            mRGB.copyTo(output);
        }

        processOutput();

        image.close();
    }

    public void invertDisplayIntermediate() {
        displayIntermediate = !displayIntermediate;
    }

    public void rescan() {
        imageProcessor.reScan();
    }

    public void setNumberClassifier(INumberClassifier classifier) {
        imageProcessor.setNumberClassifier(classifier);
    }

    private void processInput(@NotNull ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        if (nv21 == null) {
            nv21 = new byte[ySize + uSize + vSize];
        }

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        if (mYuv == null) {
            mYuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        }

        mYuv.put(0, 0, nv21);

        if (mRGB == null) {
            mRGB = new Mat();
        }

        Imgproc.cvtColor(mYuv, mRGB, Imgproc.COLOR_YUV2RGB_NV21, 3);

        Core.rotate(mRGB, mRGB, Core.ROTATE_90_CLOCKWISE);

        Imgproc.resize(mRGB, mRGB, squared);

        Log.d(TAG, "new image size : " + mRGB.size().toString());
        //outputs 720 x 720
        image.close();
    }

    private void processOutput() {
        if (bmp == null) {
            bmp = Bitmap.createBitmap(output.cols(), output.rows(), Bitmap.Config.ARGB_8888);
        }
        Utils.MatToBitmap(output, bmp);

        previewView.post(() -> previewView.setImage(bmp));
    }

}
