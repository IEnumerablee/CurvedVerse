package org.i212.curvedverse.util.ifs;

import org.i212.curvedverse.util.ComplexNumber;

import java.util.ArrayList;
import java.util.List;

public class DragonCurveStrategy extends IFSStrategy {

    public DragonCurveStrategy() {
        super();
    }

    @Override
    protected List<Transformation> defineTransformations() {
        List<Transformation> transforms = new ArrayList<>();

        ComplexNumber k1 = new ComplexNumber(0.5, 0.5);
        ComplexNumber b1 = new ComplexNumber(0, 0);
        transforms.add(new Transformation(k1, b1));

        ComplexNumber k2 = new ComplexNumber(0.5, -0.5);
        ComplexNumber b2 = new ComplexNumber(1, 0);
        transforms.add(new Transformation(k2, b2));


        ComplexNumber minusOne = new ComplexNumber(-1, 0);

        transforms.add(new Transformation(k1.multiply(minusOne), new ComplexNumber(0, 1)));
        transforms.add(new Transformation(k2.multiply(minusOne), new ComplexNumber(1, -1)));

        return transforms;
    }

    @Override
    public String getName() {
        return "DragonCurve";
    }
}

