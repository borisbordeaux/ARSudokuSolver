package com.borisbordeaux.arsudokusolver.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Sudoku {

    //81 cells for the sudoku
    private final Cell[] cells = new Cell[81];

    //the different groups of the sudoku
    private final Group[] lines = new Group[9];
    private final Group[] columns = new Group[9];
    private final Group[] bigCells = new Group[9];

    //a list of the lists of the indices of the played cells when a choice is made
    //the first value for each list is the index of the cell where a choice was made
    private final ArrayList<ArrayList<Integer>> playedCellsAfterChoice;

    //used to force the end of a sudoku when there are unresolvable errors
    //aka errors not due to a choice
    private boolean forceEnd;

    /**
     * Constructs an empty sudoku with all groups initialized
     */
    public Sudoku() {
        for (int i = 0; i < 81; i++) {
            cells[i] = new Cell();
        }
        playedCellsAfterChoice = new ArrayList<>();
        fillGroups();
        reset();
    }

    /**
     * Sets the given value for the cell at the given index
     *
     * @param index the index of the cell, must be in [0..80]
     * @param value the value to set, must be in [0..9]
     */
    public void setValue(int index, int value) {
        if (value > -1 && value < 10 && index > -1 && index < 81) {
            cells[index].setValue(value);
        }
    }

    /**
     * Sets the given value as default value for the cell at the given index
     *
     * @param index the index of the cell, must be in [0..80]
     * @param value the value to set, must be in [0..9]
     */
    public void setInitValue(int index, int value) {
        if (value > -1 && value < 10 && index > -1 && index < 81) {
            cells[index].setInitValue(value);
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
                if (cells[i].getValue() == 0) {
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
        for (Cell c : cells) {
            c.reset();
        }
        forceEnd = false;
        playedCellsAfterChoice.clear();
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
            if (lines[i].isError() || columns[i].isError() || bigCells[i].isError()) {
                error = true;
            }
        }
        return error;
    }

    /**
     * Getter for the value of the cell at the given index
     *
     * @param index the index of the cell
     * @return the value of the cell at the given index
     */
    public int getValue(int index) {
        return cells[index].getValue();
    }

    /**
     * Indicates whether the cell at the given index has a default value or not
     *
     * @param index the index of the cell
     * @return true if the cell has a default value, false otherwise
     */
    public boolean isInitValue(int index) {
        return cells[index].isInitValue();
    }

    /**
     * Fills the groups (lines, columns, big cells) of the sudoku with the cells
     * stored in the array, it simplify the solving of the sudoku
     */
    private void fillGroups() {
        for (int i = 0; i < 9; i++) {
            //lines
            lines[i] = new Group();
            for (int j = i * 9; j < (i + 1) * 9; j++) {
                lines[i].addCell(cells[j]);
            }

            //columns
            columns[i] = new Group();
            for (int j = i; j < 81; j += 9) {
                columns[i].addCell(cells[j]);
            }

            //big cells
            bigCells[i] = new Group();
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    bigCells[i].addCell(cells[i * 3 + j * 9 + k + (18 * (i / 3))]);
                }
            }
        }
    }

    /**
     * Sets automatically a value for each cell which has only one possible value.
     * Adds all cells which has changed to the last list of the played cells
     * if a choice was made earlier
     *
     * @return true if there was a change, false otherwise
     */
    private boolean setAutoValues() {
        boolean change = false;
        for (int i = 0; i < 81; i++) {
            if (cells[i].setAutoValue()) {
                change = true;
                if (playedCellsAfterChoice.size() != 0) {
                    playedCellsAfterChoice.get(playedCellsAfterChoice.size() - 1).add(i);
                }
            }
        }
        return change;
    }

    /**
     * Chooses the cell with the less possible values, and then
     * chose a random value for it among its possible values.
     * Error can occur if all the cells having a value of 0
     * have 0 possible value
     *
     * @return false if no error was detected, true otherwise
     */
    private boolean choseRandomValue() {
        boolean error = false;
        int chosenCell = -1;
        int minNbPosVal = 10;
        for (int i = 0; i < 81; i++) {
            int nbPosVal = cells[i].getNbPossibleValues();
            if (nbPosVal < minNbPosVal && nbPosVal != 0 && cells[i].getValue() == 0) {
                chosenCell = i;
                minNbPosVal = nbPosVal;
            }
        }
        if (chosenCell != -1) {
            cells[chosenCell].setRandomValue();

            ArrayList<Integer> l = new ArrayList<>();
            l.add(chosenCell);
            playedCellsAfterChoice.add(l);
        } else {
            error = true;
        }
        return error;
    }

    /**
     * Updates the possible values of all cells
     */
    private void updatePossibleValues() {
        for (Cell c : cells) {
            c.resetPossibleValues();
        }
        for (int i = 0; i < 9; i++) {
            lines[i].updatePossibleValues();
            columns[i].updatePossibleValues();
            bigCells[i].updatePossibleValues();
        }
    }

    /**
     * When the sudoku is on error, if no choice was made, forces the end of resolution.
     * If at least one choice was made, add the chosen value to the forbidden values of
     * the cell and resets the value of all played cells since that choice to 0 (including
     * the cell on which the choice was made). Moreover, resets all forbidden values for
     * each of these cells (excluding the cell on which the choice was made)
     */
    private void errorHandling() {
        if (playedCellsAfterChoice.size() != 0) {
            //get first cell played after choice, hence the cell on which there was a choice
            int index = playedCellsAfterChoice.get(playedCellsAfterChoice.size() - 1).get(0);

            int forbiddenVal = cells[index].getValue();
            cells[index].setForbiddenValue(forbiddenVal);

            //reset played values since choice
            for (Integer i : playedCellsAfterChoice.get(playedCellsAfterChoice.size() - 1)) {
                cells[i].setValue(0);
                if (i != index) {
                    cells[i].resetForbiddenValues();
                }
            }

            playedCellsAfterChoice.remove(playedCellsAfterChoice.size() - 1);
        } else {
            forceEnd = true;
        }
    }
}
