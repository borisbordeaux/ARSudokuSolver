package com.borisbordeaux.arsudokusolver.model;

import java.util.ArrayList;

public class Group {

    //all the cells of the group
    private final ArrayList<Cell> cells;

    /**
     * Construct an empty group
     */
    public Group() {
        cells = new ArrayList<>();
    }

    /**
     * Adds a cell to the group
     *
     * @param c the cell to add to the group
     */
    public void addCell(Cell c) {
        cells.add(c);
    }

    /**
     * Updates the possible values of the cells
     * depending on the current values of the cells
     */
    public void updatePossibleValues() {
        for (Cell c : cells) {
            int v = c.getValue();
            if (v != 0) {
                for (Cell c2 : cells) {
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
        for (Cell c : cells) {
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
