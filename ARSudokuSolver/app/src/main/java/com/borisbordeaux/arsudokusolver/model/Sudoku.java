package com.borisbordeaux.arsudokusolver.model;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Sudoku {

    //TAG for debugging logs
    private final String TAG = "Solve";

    //81 cases for the sudoku
    private final Case[] cases;

    //the different groups of the sudoku
    private final Group[] lines;
    private final Group[] columns;
    private final Group[] bigCases;

    //a list of the lists of the indices of the played cases when a choice is made
    //the first value for each list is the index of the case where a choice was made
    private final ArrayList<ArrayList<Integer>> playedCasesAfterChoice;

    //used to force the end of a sudoku when there are unresolvable errors
    //aka errors not due to a choice
    private boolean forceEnd;

    /**
     * Constructor
     */
    public Sudoku() {
        cases = new Case[81];
        lines = new Group[9];
        columns = new Group[9];
        bigCases = new Group[9];
        for (int i = 0; i < 81; i++) {
            cases[i] = new Case();
        }
        fillGroups();
        playedCasesAfterChoice = new ArrayList<>();
        reset();
    }

    /**
     * Sets the given value for the case at the given index
     *
     * @param index the index of the case, must be in [0..80]
     * @param value the value to set, must be in [0..9]
     */
    public void setValue(int index, int value) {
        if (value > -1 && value < 10 && index > -1 && index < 81) {
            cases[index].setValue(value);
        }
    }

    /**
     * Sets the given value as default value for the case at the given index
     *
     * @param index the index of the case, must be in [0..80]
     * @param value the value to set, must be in [0..9]
     */
    public void setInitValue(int index, int value) {
        if (value > -1 && value < 10 && index > -1 && index < 81) {
            cases[index].setInitValue(value);
        }
    }

    /**
     * Indicates whether the sudoku is complete or not
     *
     * @return true if the sudoku is complete, false otherwise
     */
    public boolean ended() {
        boolean res = true;
        if (!forceEnd) {
            int i = 0;
            while (i != 81) {
                if (cases[i].getValue() == 0) {
                    res = false;
                    break;
                }
                i++;
            }
        }
        return res;
    }

    /**
     * Resets the sudoku
     */
    public void reset() {
        for (Case c : cases) {
            c.reset();
        }
        forceEnd = false;
        playedCasesAfterChoice.clear();
    }

    /**
     * Solves the sudoku if it is a valid one
     */
    public void solve(@NotNull int[] values) {
        if (values.length == 81) {
            //reset the sudoku
            reset();
            //set default values from the array
            for (int i = 0; i < 81; i++) {
                if (values[i] != 0) {
                    setInitValue(i, values[i]);
                } else {
                    setValue(i, values[i]);
                }
            }

            //if the sudoku is valid
            if (!isError()) {
                //solve it until it is finished
                while (!ended()) {
                    solveStep();
                }
            } else {
                Log.d("Solve", "Errors detected...");
            }
        }
    }

    /**
     * Solves the sudoku for one pass
     */
    private void solveStep() {

        updatePossibleValues();

        boolean change = setAutoValues();

        boolean error = false;

        if (!change) {
            error = choseRandomValue();
        }

        if (!error)
            error = isError();

        if (error) {
            errorHandling();
        }
    }

    /**
     * Indicates whether the sudoku has an error or not
     *
     * @return true if there is an error, false otherwise
     */
    public boolean isError() {
        boolean error = false;
        for (int i = 0; i < 9; i++) {
            if (lines[i].isError() || columns[i].isError() || bigCases[i].isError()) {
                error = true;
            }
        }
        return error;
    }

    /**
     * Getter for the value of the case at the given index
     *
     * @param index the index of the case
     * @return the value of the case at the given index
     */
    public int getValue(int index) {
        return cases[index].getValue();
    }

    /**
     * Indicates whether the case at the given index has a default value or not
     *
     * @param index the index of the case
     * @return true if the case has a default value, false otherwise
     */
    public boolean isInitValue(int index) {
        return cases[index].isInitValue();
    }

    /**
     * Fills the groups (lines, columns, big cases) of the sudoku with the cases
     * stored in the array, it simplify the solving of the sudoku
     */
    private void fillGroups() {
        for (int i = 0; i < 9; i++) {
            //lines
            lines[i] = new Group();
            for (int j = i * 9; j < (i + 1) * 9; j++) {
                lines[i].addCase(cases[j]);
            }

            //columns
            columns[i] = new Group();
            for (int j = i; j < 81; j += 9) {
                columns[i].addCase(cases[j]);
            }

            //big cases
            bigCases[i] = new Group();
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    bigCases[i].addCase(cases[i * 3 + j * 9 + k + (18 * (i / 3))]);
                }
            }
        }
    }

    /**
     * Sets automatically a value for each case which has only one possible value.
     * Adds all cases which has changed to the last list of the played cases
     * if a choice was made earlier
     *
     * @return true if there was a change, false otherwise
     */
    private boolean setAutoValues() {
        boolean change = false;
        for (int i = 0; i < 81; i++) {
            if (cases[i].setAutoValue()) {
                Log.d(TAG, "Put value in case " + i + " : " + cases[i].getValue());
                change = true;
                if (playedCasesAfterChoice.size() != 0) {
                    playedCasesAfterChoice.get(playedCasesAfterChoice.size() - 1).add(i);
                }
            }
        }
        return change;
    }

    /**
     * Chooses the case with the less possible values, and then
     * chose a random value for it among its possible values.
     * Error can occur if all the cases having a value of 0
     * have 0 possible value
     *
     * @return false if no error was detected, true otherwise
     */
    private boolean choseRandomValue() {
        boolean error = false;
        Log.d(TAG, "No change : choice of random value...");
        int chosenCase = -1;
        int minNbPosVal = 10;
        for (int i = 0; i < 81; i++) {
            int nbPosVal = cases[i].getNbPossibleValues();
            if (nbPosVal < minNbPosVal && nbPosVal != 0 && cases[i].getValue() == 0) {
                chosenCase = i;
                minNbPosVal = nbPosVal;
            }
        }
        if (chosenCase != -1) {
            cases[chosenCase].setRandomValue();
            Log.d(TAG, "chosen value for case " + chosenCase + " : " + cases[chosenCase].getValue());

            ArrayList<Integer> l = new ArrayList<>();
            l.add(chosenCase);
            playedCasesAfterChoice.add(l);
        } else {
            error = true;
        }
        return error;
    }

    /**
     * Updates the possible values for all cases of the sudoku
     */
    private void updatePossibleValues() {
        for (Case c : cases) {
            c.resetPossibleValues();
        }
        for (int i = 0; i < 9; i++) {
            lines[i].updatePossibleValues();
            columns[i].updatePossibleValues();
            bigCases[i].updatePossibleValues();
        }
    }

    /**
     * When the sudoku is on error, if no choice was made, forces the end of resolution.
     * If at least on choice was made, add the value chosen to the forbidden values of
     * the case and sets the value to all played case since the choice to 0 (including
     * the case on which the choice was made). Moreover, resets all forbidden values for
     * each of these cases but the case on which the choice was made
     */
    private void errorHandling() {
        Log.d(TAG, "Errors in sudoku...");
        Log.d(TAG, "Number of choices done : " + playedCasesAfterChoice.size());
        if (playedCasesAfterChoice.size() != 0) {
            int index = playedCasesAfterChoice.get(playedCasesAfterChoice.size() - 1).get(0);

            int forbiddenVal = cases[index].getValue();

            Log.d(TAG, "Reset played values since change...");
            for (Integer i : playedCasesAfterChoice.get(playedCasesAfterChoice.size() - 1)) {
                cases[i].setValue(0);
                if (i != index) {
                    cases[i].resetForbiddenValues();
                    Log.d(TAG, "Case " + i);
                }
            }

            Log.d(TAG, "Add to forbidden values of last chosen value for the case " + index + " : " + forbiddenVal);
            cases[index].setForbiddenValue(forbiddenVal);

            Log.d(TAG, "Fin du reset");
            playedCasesAfterChoice.remove(playedCasesAfterChoice.size() - 1);
        } else {
            Log.d(TAG, "No more choices, forced end");
            forceEnd = true;
        }
    }
}
