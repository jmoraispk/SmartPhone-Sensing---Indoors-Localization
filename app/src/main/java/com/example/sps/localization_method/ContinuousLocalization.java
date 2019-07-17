package com.example.sps.localization_method;


import com.example.sps.map.WallPositions;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ContinuousLocalization extends LocalizationMethod {

    CopyOnWriteArrayList<Particle> spreadParticles(float[] priorBelief);

    void updateParticles(float azi, float distance, CopyOnWriteArrayList<Particle> particles);
    void collideAndResample(CopyOnWriteArrayList<Particle> particles, WallPositions walls);
}
