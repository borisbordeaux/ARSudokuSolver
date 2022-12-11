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
     * Creates a {@link Bitmap} image based on the data of the given {@link Mat}
     *
     * @param img the {@link Mat} the {@link Bitmap} will be based on
     * @return the {@link Bitmap} created
     */
    public static Bitmap MatToBitmap(@NotNull Mat img) {
        Bitmap bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(img, bmp);
        return bmp;
    }

    /**
     * Fills a {@link Bitmap} image based on the data of the given {@link Mat}
     *
     * @param img the {@link Mat} the {@link Bitmap} will be based on
     * @param bmp the {@link Bitmap} that will be filled
     */
    public static void MatToBitmap(@NotNull Mat img, @NotNull Bitmap bmp) {
        org.opencv.android.Utils.matToBitmap(img, bmp);
    }

    /**
     * Converts given {@link ImageProxy} YUV image into RGB to fill the given {@link Mat}
     *
     * @param src the YUV image to convert
     * @param dst the {@link Mat} that will contain the RGB version of the image
     */
    public static void convYUV2RGB(@NotNull ImageProxy src, @NotNull Mat dst) {
        ByteBuffer yBuffer = src.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = src.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = src.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21Data = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21Data, 0, ySize);
        vBuffer.get(nv21Data, ySize, vSize);
        uBuffer.get(nv21Data, ySize + vSize, uSize);

        Mat mYuv = new Mat(src.getHeight() + src.getHeight() / 2, src.getWidth(), CvType.CV_8UC1);
        mYuv.put(0, 0, nv21Data);
        Imgproc.cvtColor(mYuv, dst, Imgproc.COLOR_YUV2RGB_NV21, 3);
        mYuv.release();
    }
}
