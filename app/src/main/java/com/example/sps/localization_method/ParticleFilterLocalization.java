package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.database.DatabaseService;
import com.example.sps.map.Cell;
import com.example.sps.map.WallPositions;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.example.sps.LocateMeActivity.NUM_CELLS;

public class ParticleFilterLocalization implements ContinuousLocalization {

    public static final int NUM_PARTICLES = 3000;
    private NormalDistribution noiseDegrees = new NormalDistribution(0, 90/4); //before 22.5
    private UniformRealDistribution noiseDistance;

    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, DatabaseService databaseService) {
        return new float[0];
    }

    @Override
    public String getMiscInfo() {
        return "NUM_PARTICLES = " + NUM_PARTICLES;
    }

    //Choose a particle amongst various particles with various weights in the same cell.
    private Particle sampleParticle(EnumeratedDistribution distribution, Random r, float weight) {

        WallPositions walls = new WallPositions();


        int cell = (int) distribution.sample();

        Cell cellO = walls.getCells().get(cell);

        float sampleX = r.nextFloat() * (cellO.getRightWall() - cellO.getLeftWall()) + cellO.getLeftWall();
        float sampleY = r.nextFloat() * (cellO.getBottomWall() - cellO.getTopWall()) + cellO.getTopWall();

        return new Particle(sampleX, sampleY, weight, cell);

    }

    @Override
    public CopyOnWriteArrayList<Particle> spreadParticles(float[] priorBelief) {
        // If prior belief is uniform, distribute by area. Otherwise, by belief.
        float[] spreadProbabilities = priorBelief;
        boolean allEqual = true;
        for (int i = 0; i < priorBelief.length; i++)
            if (priorBelief[Math.max(0, i - 1)] != priorBelief[i]) {
                allEqual = false;
                break;
            }
        if (allEqual) {
            WallPositions wallPositions = new WallPositions();
            for (int i = 0; i < priorBelief.length; i++) {
                spreadProbabilities[i] = wallPositions.getCells().get(i).getAreaOfCell() / wallPositions.getTotalArea();
            }
        }

        CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

        WallPositions walls = new WallPositions();

        Random r = new Random(System.currentTimeMillis());
        List<Pair<Integer, Double>> itemWeights = new ArrayList<>();
        for (int i = 0; i < spreadProbabilities.length; i++) {
            itemWeights.add(new Pair(i, new Double(spreadProbabilities[i])));
        }
        EnumeratedDistribution cellProbability = new EnumeratedDistribution(itemWeights);

        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(sampleParticle(cellProbability, r, 1));

        }
        return particles;
    }

    //Update particle positions based on the distance and direction they should move + noise
    @Override
    public void updateParticles(float azi, float distance, CopyOnWriteArrayList<Particle> particles) {
        if (distance == 0)
            return;


        noiseDistance = new UniformRealDistribution(-0.2, 0.2);

        float norm;
        float angle;
        float toRadians = (float) (1.0f / 180 * Math.PI);
        for (Particle p : particles) {
            p.incTimeAlive();
            norm = (float) (distance + noiseDistance.sample());
            angle = (float) (azi + noiseDegrees.sample());
            p.setX((float) (p.getX() + norm * Math.cos(angle * toRadians)));
            p.setY((float) (p.getY() + norm * Math.sin(angle * toRadians)));
        }
    }

    @Override
    public void collideAndResample(CopyOnWriteArrayList<Particle> particles, WallPositions walls) {
        LinkedList<Particle> deadParticles = new LinkedList<>();


        //collide and erase
        for (Particle p : particles) {
            if (walls.getDrawable().get(p.getCell()).collide(p))
                deadParticles.add(p);
        }
        particles.removeAll(deadParticles);

        int totalTimeAlive = 0;
        boolean cellFound;
        int cell_slack = 4; //check collisions in cells in the proximity (+-cell_slack)
        for (Particle p : particles) {
            cellFound = false;
            for (int i = Math.max(0, p.getCell() - cell_slack); i < Math.min(walls.getDrawable().size(), p.getCell() + cell_slack + 1); i++) {
                if (walls.getDrawable().get(i).isParticleInside(p)) {
                    p.setCell(i);
                    cellFound = true;
                    totalTimeAlive += p.getTimeAlive();
                    break;
                }
            }
            if (!cellFound) {
                deadParticles.add(p);
            }
        }
        totalTimeAlive += deadParticles.size(); //All dead particles have time alive 1

        particles.removeAll(deadParticles);

        if (particles.size() != 0) {
            List<Pair<Particle, Double>> particleWeights = new ArrayList<>();
            for (Particle p : particles) {
                p.setWeight(((float) p.getTimeAlive()) / totalTimeAlive);
                particleWeights.add(new Pair<Particle, Double>(p, (double) p.getWeight()));
            }

            EnumeratedDistribution<Particle> distribution = new EnumeratedDistribution<>(particleWeights);

            Random r = new Random(System.currentTimeMillis());


            List<Particle> newParticles = new ArrayList<>(deadParticles.size());

            float[] areaRatio = new float[NUM_CELLS];

            WallPositions wallPositions = new WallPositions();
            List<Pair<Integer, Double>> areaDistributionWeights = new ArrayList<>();

            for (int i = 0; i < areaRatio.length; i++) {
                areaDistributionWeights.add(new Pair<Integer, Double>(i, (double) (wallPositions.getCells().get(i).getAreaOfCell() / wallPositions.getTotalArea())));
            }
            EnumeratedDistribution<Integer> areaDistribution = new EnumeratedDistribution<>(areaDistributionWeights);
            int startRandomSpreadIndex = (int) Math.floor(deadParticles.size() * 1);


            for (int i = 0; i < deadParticles.size(); i++) {
                if (i < startRandomSpreadIndex) {
                    Particle toCopy = distribution.sample();
                    newParticles.add(new Particle(toCopy.getX(), toCopy.getY(), 1.0f/totalTimeAlive, toCopy.getCell()));
                } else {
                    newParticles.add(sampleParticle(areaDistribution, r, 1.0f / totalTimeAlive));
                }
            }

            particles.addAll(newParticles);
        } else {
            float[] belief = new float[NUM_CELLS];
            particles = spreadParticles(belief);
        }
    }

}
