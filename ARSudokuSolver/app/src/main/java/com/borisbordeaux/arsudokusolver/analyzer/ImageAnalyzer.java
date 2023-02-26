package com.borisbordeaux.arsudokusolver.analyzer;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageProxy;

import com.borisbordeaux.arsudokusolver.classifier.INumberClassifier;
import com.borisbordeaux.arsudokusolver.utils.image.ImageConverter;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImageAnalyzer implements Analyzer {

    private final Size SQUARE_SIZE = new Size(480, 480);
    private final ImageView mPreviewView;
    private final ImageProcessor mImageProcessor = new ImageProcessor();
    private boolean mDisplayIntermediate = false;
    private Bitmap bmp;
    private Mat rgb;
    private Mat output;

    /**
     * Constructor, initializes the preview on which the result will be drawn
     *
     * @param view the view that will display the result
     */
    public ImageAnalyzer(ImageView view) {
        this.mPreviewView = view;
    }

    /**
     * {@inheritDoc}
     *
     * @param image the image to analyze
     */
    @Override
    public void analyze(@NotNull ImageProxy image) {

        if (image.getFormat() != ImageFormat.YUV_420_888) {
            image.close();
            Toast t = Toast.makeText(mPreviewView.getContext(), "", Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            t.setText("Image format not compatible");
            t.show();
            return;
        }

        //convert image Yuv to Mat RGB
        //rotate the image because native image is rotated
        //resize to a square image
        if (rgb == null)
            rgb = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC3);
        ImageConverter.convYUV2RGB(image, rgb);
        Imgproc.resize(rgb, rgb, SQUARE_SIZE);

        //fill output image
        if (output == null)
            output = new Mat(SQUARE_SIZE, CvType.CV_8UC3);
        if (mImageProcessor.getNumberClassifier() != null) {
            if (mDisplayIntermediate) {
                mImageProcessor.getIntermediate(rgb, output);
            } else {
                mImageProcessor.getFinalImage(rgb, output);
            }
        } else {
            rgb.copyTo(output);
        }

        //display output image
        if (bmp == null)
            bmp = Bitmap.createBitmap(output.cols(), output.rows(), Bitmap.Config.ARGB_8888);
        ImageConverter.MatToBitmap(output, bmp);
        mPreviewView.post(() -> mPreviewView.setImageBitmap(bmp));

        //free memory
        image.close();
        rgb.release();
        output.release();
    }

    /**
     * Toggles the output of the analysis to display intermediate image or final image
     */
    public void toggleDisplayIntermediate() {
        mDisplayIntermediate = !mDisplayIntermediate;
    }

    /**
     * Forces a scan of the grid and the sudoku resolution
     * the next time a grid is detected on the screen
     */
    public void rescan() {
        mImageProcessor.reScan();
    }

    /**
     * Setter for the number classifier to use when processing the image
     *
     * @param classifier the classifier to set
     */
    public void setNumberClassifier(INumberClassifier classifier) {
        mImageProcessor.setNumberClassifier(classifier);
    }

}
