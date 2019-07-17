package com.example.sps.data_collection;

class ScanInfo {

    private boolean scanSuccessful;
    private int cellId;
    private Direction direction;

    public ScanInfo(boolean wasScanSuccessful, int cellId, Direction direction) {
        this.scanSuccessful = wasScanSuccessful;
        this.cellId = cellId;
        this.direction = direction;
    }

    public boolean isScanSuccessful() {
        return scanSuccessful;
    }

    public void setScanSuccessful(boolean wasScanSuccessful) {
        this.scanSuccessful = wasScanSuccessful;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
