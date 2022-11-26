package com.borisbordeaux.arsudokusolver.utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;

public class Utils {

    private static final String TAG = "Serialization";

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
     * Fill a bitmap image based on the data of the given Mat
     *
     * @param img the Mat the bitmap will be based on
     * @param bmp the Bitmap that will be filled
     */
    public static void MatToBitmap(@NotNull Mat img, @NotNull Bitmap bmp) {
        org.opencv.android.Utils.matToBitmap(img, bmp);
    }

    /**
     * Saves the given Mat to the given path with the given name
     *
     * @param img   the Mat to save
     * @param dir   the path of the directory (with the last '/')
     * @param filename the name of the file with its extension (jpg)
     */
    public static void saveMat(@NotNull Mat img, @NotNull String dir, @NotNull String filename) {
        saveBitmap(MatToBitmap(img), dir, filename);
    }

    /**
     * Saves the given Mat to the given directory with the given name.
     *
     * @param bmp   the bitmap image to save
     * @param dir   the path of the directory (with the last '/')
     * @param filename the name of the file with its extension (jpg)
     */
    public static void saveBitmap(@NotNull Bitmap bmp, @NotNull String dir, @NotNull String filename) {
        File directory = new File(dir);

        Log.d(TAG, directory.getAbsolutePath());
        if (!directory.exists()) {
            if (directory.mkdir()) {
                Log.d(TAG, "Directory created");
            } else {
                Log.d(TAG, "Directory not created");
            }
        }

        File file = new File(directory, filename);

        try {
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
