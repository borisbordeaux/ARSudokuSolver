package com.borisbordeaux.arsudokusolver.model;

import java.util.ArrayList;

public class Group {

    //all the cases of the group
    private final ArrayList<Case> cases;

    /**
     * Constructor
     */
    public Group() {
        cases = new ArrayList<>();
    }

    /**
     * Adds a case to the group
     *
     * @param c the case to add to the group
     */
    public void addCase(Case c) {
        cases.add(c);
    }

    /**
     * Updates the possible values of the cases
     * depending on the current values of the cases
     */
    public void updatePossibleValues() {
        for (Case c : cases) {
            int v = c.getValue();
            if (v != 0) {
                for (Case c2 : cases) {
                    c2.removePossibleValue(v);
                }
            }
        }
    }

    /**
     * Indicates, as an error, whether the group contains
     * two or more same digits (from 1 to 9, 0 is skipped)
     *
     * @return true if there is an error, false otherwise
     */
    public boolean isError() {
        boolean error = false;
        ArrayList<Integer> list = new ArrayList<>();
        for (Case c : cases) {
            int val = c.getValue();
            if (val != 0) {
                if (list.contains(val)) {
                    error = true;
                } else {
                    list.add(c.getValue());
                }
            }
        }
        return error;
    }

}
