package com.example.sps.map;

import com.example.sps.localization_method.Particle;
import com.example.sps.localization_method.ParticleFilterLocalization;

public class Cell {

    //Enum of the lazy
    public static final int TOP = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int LEFT= 3;

    float[] walls;
    boolean[] traversable;
    int cellNumber;

    public Cell(int cellNumber, float[] walls, boolean[] traversable) {
        this.walls = walls;
        this.cellNumber = cellNumber;
        this.traversable = traversable;
    }

    public float getAreaOfCell() {
        return (getBottomWall() - getTopWall()) * (getRightWall() - getLeftWall());
    }

    public float[] getWalls() {
        return walls;
    }

    public void setWalls(float[] walls) {
        this.walls = walls;
    }

    public int getCellNumber() {
        return cellNumber;
    }

    public void setCellNumber(int cellNumber) {
        this.cellNumber = cellNumber;
    }

    public boolean[] getTraversable() {
        return traversable;
    }

    public void setTraversable(boolean[] traversable) {
        this.traversable = traversable;
    }

    public float getTopWall() {
        return walls[TOP];
    }


    public float getRightWall() {
        return walls[RIGHT];
    }


    public float getBottomWall() {
        return walls[BOTTOM];
    }


    public float getLeftWall() {
        return walls[LEFT];
    }



    /* Collision method work principle:
       1- Check if a vertical wall if between the x position of the initial and last positions.
       2- If yes: check if y positions are in the wall bottom and top positions;
       3- If yes: there is a collision.

       Does the analogous for horizontal walls.
       Fails in certain scenarios but considering the maximum moving speed, the scenarios where
       it fails it is likely that we actually don't want to compute any collision and simply account
       that as normal movement because there can be a problem with the direction vector.
    * */
    public boolean collide(Particle p) {

        if(!traversable[LEFT]) {
            if(p.getLast_x() < walls[LEFT] && walls[LEFT] < p.getX() )
                if(walls[TOP] < p.getY() && p.getY() < walls[BOTTOM])
                    return true;
            if(p.getX() < walls[LEFT] && walls[LEFT] < p.getLast_x() )
                if(walls[TOP] < p.getY() && p.getY() < walls[BOTTOM])
                    return true;
        }
        if(!traversable[RIGHT]) {
            if(p.getLast_x() < walls[RIGHT] && walls[RIGHT] < p.getX() )
                if(walls[TOP] < p.getY() && p.getY() < walls[BOTTOM])
                    return true;
            if(p.getX() < walls[RIGHT] && walls[RIGHT] < p.getLast_x() )
                if(walls[TOP] < p.getY() && p.getY() < walls[BOTTOM])
                    return true;
        }
        if(!traversable[TOP]) {
            if(p.getLast_y() < walls[TOP] && walls[TOP] < p.getY() )
                if(walls[LEFT] < p.getX() && p.getX() < walls[RIGHT])
                    return true;
            if(p.getY() < walls[TOP] && walls[TOP] < p.getLast_y() )
                if(walls[LEFT] < p.getX() && p.getX() < walls[RIGHT])
                    return true;

        }
        if(!traversable[BOTTOM]) {
            if(p.getLast_y() < walls[BOTTOM] && walls[BOTTOM] < p.getY() )
                if(walls[LEFT] < p.getX() && p.getX() < walls[RIGHT])
                    return true;
            if(p.getY() < walls[BOTTOM] && walls[BOTTOM] < p.getLast_y() )
                if(walls[LEFT] < p.getX() && p.getX() < walls[RIGHT])
                    return true;
        }
        return false;
    }

    //Returns if a particle is inside the cell
    public boolean isParticleInside(Particle p){
        return (p.getX() > walls[LEFT] && p.getX() < walls[RIGHT] && p.getY() > walls[TOP] && p.getY() < walls[BOTTOM]);
    }
}
