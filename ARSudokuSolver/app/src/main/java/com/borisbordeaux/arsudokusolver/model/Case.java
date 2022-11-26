package com.borisbordeaux.arsudokusolver.model;

import java.util.ArrayList;

public class Case {

    //the value of the case
    private int value;

    //the possible values of the case
    //if the i-th is true, then i+1 is a possible value
    private final boolean[] possibleValues;

    //the forbidden values, used when a choice is made but led to an error
    private final ArrayList<Integer> forbiddenValues;

    //indicates whether it is a default value or not
    private boolean isInitValue;

    /**
     * Constructor
     */
    public Case() {
        value = 0;
        isInitValue = false;
        possibleValues = new boolean[9];
        forbiddenValues = new ArrayList<>();
        resetPossibleValues();
    }

    /**
     * Sets the given value and set it as a default value
     *
     * @param v the value to set as default
     */
    public void setInitValue(int v) {
        isInitValue = true;
        setValue(v);
    }

    /**
     * Indicates whether the case has a default value
     *
     * @return true if it has a default value, false otherwise
     */
    public boolean isInitValue() {
        return isInitValue;
    }

    /**
     * Sets the given value as the value of the case
     *
     * @param v the value to set to the case
     */
    public void setValue(int v) {
        value = v;
    }

    /**
     * Indicates the value of the case
     *
     * @return the value of the case
     */
    public int getValue() {
        return value;
    }

    /**
     * Removes the given value from the possible values
     *
     * @param v the value to remove from the possible values
     */
    public void removePossibleValue(int v) {
        if (v > 0 && v <= 9) {
            possibleValues[v - 1] = false;
        }
    }

    /**
     * Sets all the values (1-9) but the ones which
     * are forbidden values to be possible values
     */
    public void resetPossibleValues() {
        for (int i = 0; i < 9; i++) {
            possibleValues[i] = !forbiddenValues.contains(i + 1);
        }
    }

    /**
     * If there is only one possible value, sets it.
     *
     * @return true if a value has been set, false otherwise
     */
    public boolean setAutoValue() {
        if (value == 0) {
            int indexValPos = -1;
            int nbPosVal = 0;

            for (int i = 0; i < 9; i++) {
                if (possibleValues[i]) {
                    nbPosVal++;
                    indexValPos = i;
                }
            }

            if (nbPosVal == 1) {
                value = indexValPos + 1;
            }

            return nbPosVal == 1;
        } else {
            return false;
        }
    }

    /**
     * Indicates the number of possible values of the case
     *
     * @return the number of possible values of the case
     */
    public int getNbPossibleValues() {
        int res = 0;
        for (boolean b : possibleValues) {
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
            if (possibleValues[i - 1]) {
                value = i;
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
            forbiddenValues.add(v);
        }
    }

    /**
     * Removes all forbidden values
     */
    public void resetForbiddenValues() {
        forbiddenValues.clear();
    }

    /**
     * Sets the value as 0, removes the default value, clears
     * the forbidden values and resets the possible values
     */
    public void reset() {
        setValue(0);
        isInitValue = false;
        resetForbiddenValues();
        resetPossibleValues();
    }
}
