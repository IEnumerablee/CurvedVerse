package org.i212.curvedverse.util.ifs;

import org.i212.curvedverse.util.ComplexNumber;

public class TransitionPoint extends ComplexNumber {
    private final int depth;
    private final double phase;
    private final double attractorProximity;

    public TransitionPoint(double real, double imaginary, int depth, double phase, double attractorProximity) {
        super(real, imaginary);
        this.depth = depth;
        this.phase = phase;
        this.attractorProximity = attractorProximity;
    }

    public int getDepth() {
        return depth;
    }

    public double getPhase() {
        return phase;
    }

    public double getAttractorProximity() {
        return attractorProximity;
    }

    @Override
    public String toString() {
        return "TransitionPoint{" +
                "point=" + super.toString() +
                ", depth=" + depth +
                ", phase=" + phase +
                ", attractorProximity=" + attractorProximity +
                '}';
    }
}

