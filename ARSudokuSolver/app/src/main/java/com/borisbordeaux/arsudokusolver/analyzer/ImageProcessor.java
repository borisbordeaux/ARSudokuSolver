package com.borisbordeaux.arsudokusolver.analyzer;

import com.borisbordeaux.arsudokusolver.classifier.INumberClassifier;
import com.borisbordeaux.arsudokusolver.model.Sudoku;

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

    private final Mat img;
    private final Mat rendered;
    private final MatOfPoint2f poly;
    private final Mat hierarchy;
    private final List<MatOfPoint> contours;
    private final List<MatOfPoint> contours_grille;
    private final Point[] ptsContour = new Point[4];
    private final MatOfPoint2f matPts;
    private final MatOfPoint2f matPtsContour = new MatOfPoint2f();
    private final MatOfPoint2f c2f = new MatOfPoint2f();
    private final int margin = 12;
    private final int caseSize = 80 + 2 * margin;
    private final int gridSize = 9 * caseSize;
    private final Size GRID_SIZE = new Size(gridSize, gridSize);
    private final Mat fond = Mat.zeros(gridSize, gridSize, CvType.CV_8UC3);
    private final int[] grid;
    private final Scalar WHITE = new Scalar(255, 255, 255);
    private final Scalar GREEN = new Scalar(0, 255, 0);
    private final Scalar ZERO = new Scalar(0, 0, 0);
    private final Point org = new Point();
    private final Sudoku sudoku;
    private MatOfPoint2f foundContours;
    private boolean previousGrid;
    private boolean hasToScan;
    //on cree les matrices dont on aura besoin pour ecrire
    //les nombre sous forme de masque sur l'image de rendu
    private Mat fondP;
    private INumberClassifier numberClassifier;

    public ImageProcessor() {
        img = new Mat();
        rendered = new Mat();
        foundContours = null;
        poly = new MatOfPoint2f();
        hierarchy = new Mat();
        contours = new ArrayList<>();
        contours_grille = new ArrayList<>();

        grid = new int[81];
        previousGrid = false;
        hasToScan = false;

        //on cree un contour de points en float
        //avec la liste des points des bonnes coordonnees
        //en perspective
        matPts = new MatOfPoint2f();
        matPts.fromArray(
                new Point(0, 0),
                new Point(gridSize, 0),
                new Point(gridSize, gridSize),
                new Point(0, gridSize));

        sudoku = new Sudoku();

        numberClassifier = null;
    }

    public INumberClassifier getNumberClassifier() {
        return numberClassifier;
    }

    public void setNumberClassifier(INumberClassifier numberClassifier) {
        this.numberClassifier = numberClassifier;
    }

    private void processImage(Mat inputFrame, boolean finalProcess) {
        //on copie les images de rendu pour l'affichage
        //et en noir et blanc pour la detection de contours
        inputFrame.copyTo(rendered);
        inputFrame.copyTo(img);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);

        //on effectue un effet de flou sur l'image en noir et blanc
        //cela sert a grossir les traits pour le seuillage adaptatif ensuite
        //cela cree moins de bruits, donc moins de contours
        //Imgproc.GaussianBlur(img, img, KERNEL, .5, .5, Core.BORDER_REPLICATE);
        Imgproc.adaptiveThreshold(img, img, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 13, 5);

        if (finalProcess) {
            //on vide la liste des contours
            contours.clear();

            //enfin on cherche les contours presents dans l'image
            Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            //on met a null le contour de la grille
            if (foundContours != null) {
                foundContours.release();
                foundContours = null;
            }
            //init de l'aire la plus grande des contours trouves
            double maxArea = 0;
            //pour chaque contour trouves
            for (MatOfPoint c : contours) {
                //on en calcule l'aire
                double area = Imgproc.contourArea(c);
                //si l'aire est suffisament grande pour etre gardee
                if (area > 25000) {
                    //on convertit les points du contour en float
                    c2f.fromArray(c.toArray());
                    //cela permet de calculer le perimetre (la boucle de la longueur des arcs)
                    double peri = Imgproc.arcLength(c2f, true);
                    //les contours sont constitues de beaucoup de points
                    //on approxime le contour en un contour avec le moins de points possibles
                    Imgproc.approxPolyDP(c2f, poly, 0.01 * peri, true);
                    //si on a 4 points, le contour est conserve si et seulement si
                    //son aire est plus grande que celle du contour trouve precedemment
                    //(0 si aucun contour n'est trouve avant lui)
                    if (area > maxArea && poly.total() == 4) {
                        foundContours = poly;
                        //maj de l'aire
                        maxArea = area;
                    }
                }
            }

            //si on a trouve un contour de la forme d'une grille
            //(ou plutot un quadrilatere assez grand en pratique)
            if (foundContours != null) {

                //on vide la liste des contours de grilles trouvees
                contours_grille.clear();

                //on ajoute les points du contour trouve a une liste de contours
                contours_grille.add(new MatOfPoint(foundContours.toArray()));

                //Utils.saveMat(rendered, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "step1.jpg");
                //Utils.saveMat(img, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "step2.jpg");

                //dessine le contour trouve
                Imgproc.drawContours(rendered, contours_grille, 0, GREEN, 2);

                //Utils.saveMat(rendered, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "step3.jpg");

                //on trie les points pour les avoir dans l'ordre
                //hg hd bd bg pour creer la matrice de transfomation
                //pour la perspective, le resultat une fois le tri
                //termine sera mis dans le tableau ptsContour
                sortPoints(foundContours.toList());

                //on cree un contour de points en float
                //avec la liste triee
                matPtsContour.fromArray(ptsContour);

                //on creer le matrices de transformation
                Mat transformFromSquare = Imgproc.getPerspectiveTransform(matPts, matPtsContour);
                Mat transformToSquare = Imgproc.getPerspectiveTransform(matPtsContour, matPts);

                //si pas de grille deja trouvee
                if (!previousGrid) {
                    sudoku.reset();
                }

                if (hasToScan) {
                    hasToScan = false;

                    //on transforme en carre l'image
                    Imgproc.warpPerspective(img, img, transformToSquare, GRID_SIZE);

                    //Utils.saveMat(img, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "step4.jpg");

                    //on lit les chiffres des cases
                    for (int i = 0; i < 81; i++) {
                        int rowStart = (i / 9) * caseSize;
                        int colStart = (i % 9) * caseSize;

                        //on remplit la grille
                        Mat subImage = img.submat(rowStart, rowStart + caseSize, colStart, colStart + caseSize);

                        grid[i] = numberClassifier.getNumber(subImage);
                    }

                    //on retransforme l'image
                    Imgproc.warpPerspective(img, img, transformFromSquare, rendered.size());

                    sudoku.solve(grid);
                    previousGrid = true;
                }

                fond.setTo(ZERO);

                //on ecrit en blanc dans une image noire carree
                for (int x = 0; x < 9; x++) {
                    for (int y = 0; y < 9; y++) {
                        int index = y * 9 + x;
                        if (sudoku.getValue(index) != 0) {
                            org.x = x * caseSize + margin + 3;
                            org.y = (y + 1) * caseSize - margin - 3;
                            Imgproc.putText(fond, "" + sudoku.getValue(index), org, Core.FONT_HERSHEY_PLAIN, 6, WHITE, sudoku.isInitValue(index) ? 7 : 3);
                        }
                    }
                }

                //Utils.saveMat(fond, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "step5.jpg");

                if (fondP == null) {
                    fondP = new Mat(rendered.size(), CvType.CV_8UC3);
                }

                //on applique la perspective au fond
                Imgproc.warpPerspective(fond, fondP, transformFromSquare, fondP.size());
                //fondP = resultats blancs sur noir de taille rendu

                //Utils.saveMat(fondP, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "step6.jpg");

                //on soustrait le resultat au rendu
                //cela affiche les chiffres en noir
                Core.subtract(rendered, fondP, rendered);

                //Utils.saveMat(rendered, "/storage/emulated/0/Android/data/com.borisbordeaux.arsudokusolver/files/Documents/images/", "step7.jpg");

                //on vide la memoire
                transformFromSquare.release();
                transformToSquare.release();
            } else {
                previousGrid = false;
            }

        }
    }

    public void getFinalImage(Mat inputFrame, Mat output) {
        processImage(inputFrame, true);
        rendered.copyTo(output);
    }


    public void getIntermediate(Mat inputFrame, Mat output) {
        processImage(inputFrame, false);
        img.copyTo(output);
    }

    //on va ordonner les points haut gauche, haut droite, bas gauche, bas droite
    private void sortPoints(List<Point> in) {
        //on trie, les 2 premiers points seront à gauche, et les 2 derniers à droite
        in.sort(Comparator.comparingDouble(o -> o.x));
        //on place les points à gauche et droite
        Point hg = in.get(0);
        Point bg = in.get(1);
        Point hd = in.get(2);
        Point bd = in.get(3);
        //on ajuste le haut et le bas si besoin
        if (bg.y < hg.y) {
            Point t = bg;
            bg = hg;
            hg = t;
        }
        if (bd.y < hd.y) {
            Point t = bd;
            bd = hd;
            hd = t;
        }

        ptsContour[0] = hg;
        ptsContour[1] = hd;
        ptsContour[2] = bd;
        ptsContour[3] = bg;
    }

    public void reScan() {
        previousGrid = false;
        hasToScan = true;
    }

}


