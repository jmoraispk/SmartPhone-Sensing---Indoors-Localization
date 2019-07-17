package com.example.sps.localization_method;

public class Particle {
    private float x, y;
    private float last_x, last_y;
    private float weight;
    private int cell;
    private int timeAlive;


    public Particle(float x, float y, float weight, int cell) {
        this.x = x;
        this.y = y;
        this.last_x = x;
        this.last_y = y;
        this.weight = weight;
        this.cell = cell;
        this.timeAlive = 1;
    }

    public int getTimeAlive() {
        return timeAlive;
    }

    public void incTimeAlive() {
        timeAlive++;
    }

    public void resetTimeAlive() {
        timeAlive = 1;
    }

    public float getLast_x() {
        return last_x;
    }


    public float getLast_y() {
        return last_y;
    }


    public int getCell() {
        return cell;
    }

    public void setCell(int cell) {
        this.cell = cell;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.last_x = this.x;
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.last_y = this.y;
        this.y = y;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
