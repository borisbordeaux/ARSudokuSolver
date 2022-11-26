package com.borisbordeaux.arsudokusolver.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SudokuTest {

    @Test
    public void ended() {
        Sudoku sudoku = new Sudoku();
        //default not ended
        assertFalse(sudoku.ended());

        //after solve ended
        int[] init = new int[81];
        for (int i = 0; i < 81; i++) {
            init[i] = 0;
        }
        sudoku.solve(init);
        assertTrue(sudoku.ended());

        //after solve whatever values
        for (int i = 0; i < 81; i++) {
            init[i] = i%10;
        }
        sudoku.solve(init);
        assertFalse(sudoku.ended());
    }

    @Test
    public void solve() {
        Sudoku sudoku = new Sudoku();
        int[] init = new int[81];
        for (int i = 0; i < 81; i++) {
            init[i] = 0;
        }
        sudoku.solve(init);
        assertTrue(sudoku.ended() && !sudoku.isError());
    }

    @Test
    public void isError() {
        Sudoku sudoku = new Sudoku();
        int[] init = new int[81];

        //no errors
        for (int i = 0; i < 81; i++) {
            init[i] = 0;
        }
        sudoku.solve(init);
        assertFalse(sudoku.isError());

        //all same values
        for (int i = 0; i < 81; i++) {
            init[i] = 1;
        }
        sudoku.solve(init);
        assertTrue(sudoku.isError());

        //same value in same big case
        for (int i = 0; i < 81; i++) {
            init[i] = 0;
        }
        init[0] = 4;
        init[19] = 4;
        sudoku.solve(init);
        assertTrue(sudoku.isError());

        //same value in same line
        for (int i = 0; i < 81; i++) {
            init[i] = 0;
        }
        init[0] = 4;
        init[5] = 4;
        sudoku.solve(init);
        assertTrue(sudoku.isError());

        //same value in same column
        for (int i = 0; i < 81; i++) {
            init[i] = 0;
        }
        init[2] = 4;
        init[29] = 4;
        sudoku.solve(init);
        assertTrue(sudoku.isError());
    }
}