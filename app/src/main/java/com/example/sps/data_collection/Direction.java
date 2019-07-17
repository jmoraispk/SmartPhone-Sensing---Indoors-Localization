package com.example.sps.data_collection;

public enum Direction {
    NORTH("N"),
    SOUTH("S"),
    WEST("W"),
    EAST("E");

    String dir;
    Direction(String dir){
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }
}
