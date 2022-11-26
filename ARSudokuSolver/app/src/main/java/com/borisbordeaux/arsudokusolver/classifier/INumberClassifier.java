package com.borisbordeaux.arsudokusolver.classifier;

import org.opencv.core.Mat;

public interface INumberClassifier {

    /**
     * Classifies the given image and return the number detected
     *
     * @param img the image that has to be classified
     * @return the number detected in the image
     */
    int getNumber(Mat img);
}
