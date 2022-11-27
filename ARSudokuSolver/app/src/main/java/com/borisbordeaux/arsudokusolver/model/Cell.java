package com.borisbordeaux.arsudokusolver.model;

import java.util.ArrayList;

public class Cell {

    //the possible values of the cell
    //if the i-th is true, then i+1 is a possible value
    private final boolean[] mPossibleValues;

    //the forbidden values, used when a choice is made but caused an error
    private final ArrayList<Integer> mForbiddenValues;

    //the value of the cell
    private int mValue;

    //indicates whether it is a default value or not
    private boolean mIsInitValue;

    /**
     * Construct a cell with a default value of 0
     */
    public Cell() {
        mValue = 0;
        mIsInitValue = false;
        mPossibleValues = new boolean[9];
        mForbiddenValues = new ArrayList<>();
        resetPossibleValues();
    }

    /**
     * Indicates whether the cell has a default value
     *
     * @return true if it has a default value, false otherwise
     */
    public boolean isInitValue() {
        return mIsInitValue;
    }

    /**
     * Sets the given value and set it as a default value
     *
     * @param v the value to set as default
     */
    public void setInitValue(int v) {
        mIsInitValue = true;
        setValue(v);
    }

    /**
     * Indicates the value of the cell
     *
     * @return the value of the cell
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Sets the given value as the value of the cell
     *
     * @param v the value to set to the cell
     */
    public void setValue(int v) {
        mValue = v;
    }

    /**
     * Removes the given value from the possible values
     *
     * @param v the value to remove from the possible values
     */
    public void removePossibleValue(int v) {
        if (v > 0 && v <= 9) {
            mPossibleValues[v - 1] = false;
        }
    }

    /**
     * Sets all the values (1-9) but the ones which
     * are forbidden values to be possible values
     */
    public void resetPossibleValues() {
        for (int i = 0; i < 9; i++) {
            mPossibleValues[i] = !mForbiddenValues.contains(i + 1);
        }
    }

    /**
     * If there is only one possible value, sets it.
     *
     * @return true if a value has been set, false otherwise
     */
    public boolean setAutoValue() {
        if (mValue == 0) {
            int indexValPos = -1;
            int nbPosVal = 0;

            for (int i = 0; i < 9; i++) {
                if (mPossibleValues[i]) {
                    nbPosVal++;
                    indexValPos = i;
                }
            }

            if (nbPosVal == 1) {
                mValue = indexValPos + 1;
            }

            return nbPosVal == 1;
        } else {
            return false;
        }
    }

    /**
     * Indicates the number of possible values of the cell
     *
     * @return the number of possible values of the cell
     */
    public int getNbPossibleValues() {
        int res = 0;
        for (boolean b : mPossibleValues) {
            if (b) {
                res++;
            }
        }
        return res;
    }

    /**
     * Setter for a random value among the possible values
     */
    public void setRandomValue() {
        for (int i = 9; i > 0; i--) {
            if (mPossibleValues[i - 1]) {
                mValue = i;
            }
        }
    }

    /**
     * Sets the given value as forbidden so it won't be added to possibles values
     *
     * @param v the value to be forbidden
     */
    public void setForbiddenValue(int v) {
        if (v > 0 && v <= 9) {
            mForbiddenValues.add(v);
        }
    }

    /**
     * Removes all forbidden values
     */
    public void resetForbiddenValues() {
        mForbiddenValues.clear();
    }

    /**
     * Sets the value as 0, removes the default value, clears
     * the forbidden values and resets the possible values
     */
    public void reset() {
        setValue(0);
        mIsInitValue = false;
        resetForbiddenValues();
        resetPossibleValues();
    }
}
