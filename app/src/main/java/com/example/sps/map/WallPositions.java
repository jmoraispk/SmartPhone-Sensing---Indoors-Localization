package com.example.sps.map;

import java.util.LinkedList;
import java.util.List;

public class WallPositions {


    private List<Cell> cells;

    private List<Cell> drawable;


    public WallPositions() {
        cells = new LinkedList<>();
        drawable = new LinkedList<>();
        cells.add(new Cell(1, new float[]{8.2f, 40f, 14.3f, 36f}, new boolean[]{true, false, false, false}));
        cells.add(new Cell(2, new float[]{6.1f, 40f, 8.2f, 36f}, new boolean[]{true, false, true, true}));
        cells.add(new Cell(3, new float[]{0f, 40f, 6.1f, 36f}, new boolean[]{false, false, true, false}));
        cells.add(new Cell(4, new float[]{6.1f, 36f, 8.2f, 32f}, new boolean[]{false, true, false, true}));
        cells.add(new Cell(5, new float[]{6.1f, 32f, 8.2f, 28f}, new boolean[]{false, true, false, true}));
        cells.add(new Cell(6, new float[]{6.1f, 28f, 8.2f, 24f}, new boolean[]{false, true, false, true}));
        cells.add(new Cell(7, new float[]{6.1f, 24f, 8.2f, 20f}, new boolean[]{false, true, false, true}));
        cells.add(new Cell(8, new float[]{6.1f, 20f, 8.2f, 16.0f}, new boolean[]{false, true, false, true}));
        cells.add(new Cell(9, new float[]{6.1f, 16f, 8.2f, 12.0f}, new boolean[]{false, true, false, true}));
        cells.add(new Cell(10, new float[]{6.1f, 12f, 8.2f, 8.0f}, new boolean[]{true, true, false, true}));
        cells.add(new Cell(11, new float[]{0f, 12f, 6.1f, 8.0f}, new boolean[]{false, false, true, false}));
        cells.add(new Cell(12, new float[]{6.1f, 8f, 8.2f, 4.0f}, new boolean[]{true, true, false, true}));
        cells.add(new Cell(13, new float[]{0f, 8f, 6.1f, 4.0f}, new boolean[]{false, false, true, false}));
        cells.add(new Cell(14, new float[]{11.3f, 4f, 14.3f, 0.0f}, new boolean[]{true, false, false, false}));
        cells.add(new Cell(15, new float[]{6.1f, 4f, 8.2f, 0.0f}, new boolean[]{true, true, true, false}));
        cells.add(new Cell(16, new float[]{0f, 4f, 6.1f, 0.0f}, new boolean[]{false, false, true, false}));

        drawable.addAll(cells);
        drawable.add(new Cell(17, new float[]{8.1f,4.0f,11.3f,2.5f}, new boolean[]{true, false, true, false}));

    }

    //get total area of the map.
    public float getTotalArea() {
        float total = 0;
        for(Cell c : this.cells)
            total += c.getAreaOfCell();

        return total;
    }


    public List<Cell> getCells() {
        return cells;
    }

    public List<Cell> getDrawable() {
        return drawable;
    }

    public float getMaxWidth() {
        return 40f;
    }
}
