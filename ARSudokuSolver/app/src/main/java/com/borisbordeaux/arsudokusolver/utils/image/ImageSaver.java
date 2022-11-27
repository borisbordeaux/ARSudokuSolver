package com.borisbordeaux.arsudokusolver.utils.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;

public class ImageSaver {

    //directory obtained by context
    private final String mDocumentDir;

    /**
     * Constructor which initializes the save path using given context.
     * All files saved with this class will be in the documents directory
     * of the external files directory of the context
     *
     * @param context the context to use to get the external files directory
     */
    public ImageSaver(Context context) {
        mDocumentDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/";
    }

    /**
     * Saves the given Mat to the given directory with the given name
     *
     * @param bmp      the bitmap image to save
     * @param dir      the path of the directory (with the last '/')
     * @param filename the name of the file with its extension (jpg)
     */
    public void saveBitmap(@NotNull Bitmap bmp, @NotNull String dir, @NotNull String filename) {
        File directory = new File(mDocumentDir + dir);

        if (!directory.exists()) {
            if (!directory.mkdir()) {
                return;
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

    /**
     * Saves the given Mat to the given path with the given name
     *
     * @param img      the Mat to save
     * @param dir      the path of the directory (with the last '/')
     * @param filename the name of the file with its extension (jpg)
     */
    public void saveMat(@NotNull Mat img, @NotNull String dir, @NotNull String filename) {
        saveBitmap(ImageConverter.MatToBitmap(img), dir, filename);
    }
}
