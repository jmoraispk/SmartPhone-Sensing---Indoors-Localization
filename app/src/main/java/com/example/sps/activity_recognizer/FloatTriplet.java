package com.example.sps.activity_recognizer;

public class FloatTriplet {

    private float x;
    private float y;
    private float z;

    public FloatTriplet(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getMagnitude(){
        return (float) Math.sqrt(Math.pow(this.getX(),2) + Math.pow(this.getY(),2) + Math.pow(this.getZ(),2));
    }
}
