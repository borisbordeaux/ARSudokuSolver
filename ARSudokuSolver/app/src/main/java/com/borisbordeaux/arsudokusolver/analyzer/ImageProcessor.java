package com.borisbordeaux.arsudokusolver.analyzer;

import com.borisbordeaux.arsudokusolver.classifier.INumberClassifier;
import com.borisbordeaux.arsudokusolver.model.Sudoku;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ImageProcessor {

    private final MatOfPoint2f SQUARE_POINTS;
    private final int margin = 12;
    private final int cellSize = 80 + 2 * margin;
    private final int GRID_SIZE = 9 * cellSize;
    private final Scalar WHITE = new Scalar(255, 255, 255);
    private final Scalar GREEN = new Scalar(0, 255, 0);
    private final Sudoku sudoku = new Sudoku();
    private final Mat workingImg = new Mat();
    private boolean previousGrid = false;
    private boolean hasToScan = false;
    private INumberClassifier numberClassifier = null;

    /**
     * Constructor
     */
    public ImageProcessor() {
        //create 4 points in square for perspective transform
        SQUARE_POINTS = new MatOfPoint2f();
        SQUARE_POINTS.fromArray(new Point(0, 0), new Point(GRID_SIZE, 0), new Point(GRID_SIZE, GRID_SIZE), new Point(0, GRID_SIZE));
    }

    /**
     * Getter for the classifier
     *
     * @return the classifier
     */
    public INumberClassifier getNumberClassifier() {
        return numberClassifier;
    }

    /**
     * Setter for the classifier
     *
     * @param numberClassifier the classifier to set
     */
    public void setNumberClassifier(INumberClassifier numberClassifier) {
        this.numberClassifier = numberClassifier;
    }

    /**
     * Getter for the final image, it processes the image in input and
     * fills the output with the solved sudoku superposed on the image
     *
     * @param src the image to analyse
     * @param dst the image that will be filled
     */
    public void getFinalImage(@NotNull Mat src, @NotNull Mat dst) {
        adaptiveThreshold(src, workingImg);
        src.copyTo(dst);

        MatOfPoint2f foundContour = findContours(workingImg);

        //if a contour has been found (a big square in practice)
        if (foundContour != null) {

            //we draw it on the image
            drawContour(foundContour, dst);

            //sort points to create perspective transformations
            MatOfPoint2f matPtsContour = sortPoints(foundContour);
            Mat transformFromSquare = Imgproc.getPerspectiveTransform(SQUARE_POINTS, matPtsContour);
            Mat transformToSquare = Imgproc.getPerspectiveTransform(matPtsContour, SQUARE_POINTS);

            //if no previous grid, then it is likely a new sudoku
            if (!previousGrid) {
                sudoku.reset();
            }

            //if has to solve the sudoku
            if (hasToScan) {
                hasToScan = false;
                previousGrid = true;
                readAndSolveSudoku(workingImg, transformToSquare, transformFromSquare);
            }

            //write sudoku in dst image
            writeSudoku(dst, transformFromSquare);

            //free memory
            foundContour.release();
            matPtsContour.release();
            transformFromSquare.release();
            transformToSquare.release();
        } else { //no contour found
            previousGrid = false;
        }
    }

    /**
     * Getter for the intermediate image, which is the
     * input image with an adaptive threshold filter
     *
     * @param src the input image to get the threshold filter
     * @param dst the thresholded image that will be filled
     */
    public void getIntermediate(@NotNull Mat src, @NotNull Mat dst) {
        adaptiveThreshold(src, dst);
    }

    /**
     * Indicates that the sudoku has to be scanned and solved again
     */
    public void reScan() {
        previousGrid = false;
        hasToScan = true;
    }

    /**
     * Processes the adaptive threshold on the src {@link Mat}
     * and fills the dst {@link Mat} with the result
     *
     * @param src the {@link Mat} to filter
     * @param dst the {@link Mat} that will contain the result
     */
    private void adaptiveThreshold(@NotNull Mat src, @NotNull Mat dst) {
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 13, 5);
    }

    /**
     * Finds the max area quadrilateral contour in the given {@link Mat}
     *
     * @param src a Mat that should be thresholded with the {@link #adaptiveThreshold} function
     * @return a MatOfPoint2f containing the contour found in the image
     */
    private MatOfPoint2f findContours(@NotNull Mat src) {
        MatOfPoint2f foundContour = null;
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;

        //for each contour
        for (MatOfPoint contour : contours) {
            //compute the area
            double area = Imgproc.contourArea(contour);
            //if the area is big enough
            if (area > 25000) {
                //convert contour points to float points
                MatOfPoint2f contourFloat = new MatOfPoint2f();
                contourFloat.fromArray(contour.toArray());

                //it allows to compute the perimeter
                double peri = Imgproc.arcLength(contourFloat, true);

                //contours are made of a lot of points
                //approximation with a minimum number of points
                MatOfPoint2f poly = new MatOfPoint2f();
                Imgproc.approxPolyDP(contourFloat, poly, 0.01 * peri, true);

                //if the contour has 4 points and a bigger area that the last one found
                if (area > maxArea && poly.total() == 4) {
                    //we keep this contour
                    foundContour = poly;
                    maxArea = area;
                }
            }
        }
        return foundContour;
    }

    /**
     * Draws the given contour in the given Mat
     *
     * @param contour the contour to draw
     * @param dst     the Mat on which the contour will be drawn
     */
    private void drawContour(MatOfPoint2f contour, Mat dst) {
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(new MatOfPoint(contour.toArray()));
        Imgproc.drawContours(dst, contours, 0, GREEN, 2);
    }

    /**
     * Sorts the given MatOfPoint2f containing 4 points in the following order:
     * top left, top right, bottom right, bottom left
     *
     * @param pts the points to sort
     * @return a MatOfPoint2f of sorted points or null if there are not 4 points to sort
     */
    private MatOfPoint2f sortPoints(MatOfPoint2f pts) {

        List<Point> listPoints = pts.toList();
        MatOfPoint2f result = new MatOfPoint2f();

        if (listPoints.size() == 4) {
            //sort points left to right
            listPoints.sort(Comparator.comparingDouble(o -> o.y));

            //adjust left and right
            //first point needs to be at the left of the second
            if (listPoints.get(1).x < listPoints.get(0).x) {
                Point t = listPoints.get(1);
                listPoints.set(1, listPoints.get(0));
                listPoints.set(0, t);
            }
            //third point needs to be at the right of the fourth
            if (listPoints.get(3).x > listPoints.get(2).x) {
                Point t = listPoints.get(3);
                listPoints.set(3, listPoints.get(2));
                listPoints.set(2, t);
            }

            //create result from the list
            result.fromList(listPoints);
        }
        return result;
    }

    /**
     * Reads all 81 values in the given {@link Mat} after applying a perspective transform to get a square image.
     * Applies another perspective transform to reset the {@link Mat} in its original shape
     *
     * @param src                 the {@link Mat} on which to read the values
     * @param transformToSquare   the {@link Mat} containing the perspective transform to get a square from a quadrilateral
     * @param transformFromSquare the {@link Mat} containing the perspective transform to get a quadrilateral from a square
     */
    private void readAndSolveSudoku(@NotNull Mat src, @NotNull Mat transformToSquare, @NotNull Mat transformFromSquare) {
        Size s = src.size();
        //process the perspective transform
        Imgproc.warpPerspective(src, src, transformToSquare, new Size(GRID_SIZE, GRID_SIZE));

        int[] grid = new int[81];

        //read digits in the now square image
        for (int i = 0; i < 81; i++) {
            int rowStart = (i / 9) * cellSize;
            int colStart = (i % 9) * cellSize;

            Mat subImage = src.submat(rowStart, rowStart + cellSize, colStart, colStart + cellSize);

            grid[i] = numberClassifier.getNumber(subImage);
        }

        //process the other perspective transform to reset the image
        Imgproc.warpPerspective(src, src, transformFromSquare, s);

        sudoku.solve(grid);
    }

    /**
     * Writes all values of a sudoku in the given {@link Mat} using the given perspective transformation
     *
     * @param dst                 the {@link Mat} on which the sudoku will be drawn in black
     * @param transformFromSquare the {@link Mat} containing the perspective transform to get a quadrilateral from a square
     */
    private void writeSudoku(@NotNull Mat dst, @NotNull Mat transformFromSquare) {
        //fill a black square
        Mat blackSquare = Mat.zeros(GRID_SIZE, GRID_SIZE, CvType.CV_8UC3);
        Point origin = new Point();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                int index = y * 9 + x;
                if (sudoku.getValue(index) != 0) {
                    origin.x = x * cellSize + margin + 3;
                    origin.y = (y + 1) * cellSize - margin - 3;
                    Imgproc.putText(blackSquare, "" + sudoku.getValue(index), origin, Imgproc.FONT_HERSHEY_PLAIN, 6, WHITE, sudoku.isInitValue(index) ? 7 : 3);
                }
            }
        }

        //transform the black square
        Mat perspectiveSudoku = new Mat(dst.size(), CvType.CV_8UC3);
        Imgproc.warpPerspective(blackSquare, perspectiveSudoku, transformFromSquare, dst.size());

        //subtract the sudoku to the image
        //it displays the sudoku in black
        Core.subtract(dst, perspectiveSudoku, dst);
    }

}


