package com.borisbordeaux.arsudokusolver.utils.image;

import android.graphics.Bitmap;

import androidx.camera.core.ImageProxy;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class ImageConverter {

    /**
     * Creates a bitmap image based on the data of the given Mat
     *
     * @param img the Mat the bitmap will be based on
     * @return the bitmap created
     */
    public static Bitmap MatToBitmap(@NotNull Mat img) {
        Bitmap bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(img, bmp);
        return bmp;
    }

    /**
     * Fills a bitmap image based on the data of the given Mat
     *
     * @param img the Mat the bitmap will be based on
     * @param bmp the Bitmap that will be filled
     */
    public static void MatToBitmap(@NotNull Mat img, @NotNull Bitmap bmp) {
        org.opencv.android.Utils.matToBitmap(img, bmp);
    }

    /**
     * Converts given ImageProxy YUV image into RGB to fill the given Mat
     *
     * @param image the YUV image to convert
     * @param rgb   the Mat that will contain the RGB version of the image
     */
    public static void convYUV2RGB(@NotNull ImageProxy image, @NotNull Mat rgb) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21Data = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21Data, 0, ySize);
        vBuffer.get(nv21Data, ySize, vSize);
        uBuffer.get(nv21Data, ySize + vSize, uSize);

        Mat mYuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        mYuv.put(0, 0, nv21Data);
        Imgproc.cvtColor(mYuv, rgb, Imgproc.COLOR_YUV2RGB_NV21, 3);
        mYuv.release();
    }
}