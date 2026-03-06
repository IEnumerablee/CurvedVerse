package org.i212.curvedverse.util.ifs;

import org.i212.curvedverse.util.ComplexNumber;
import java.util.Optional;

public class Transformation {
    private final ComplexNumber scale;
    private final ComplexNumber translation;

    public Transformation(ComplexNumber scale, ComplexNumber translation) {
        this.scale = scale;
        this.translation = translation;
    }

    public ComplexNumber apply(ComplexNumber z) {
        return z.multiply(scale).add(translation);
    }

    public ComplexNumber applyInverse(ComplexNumber z) {
        if (scale.abs() < 1e-10) {
            throw new IllegalStateException("Cannot apply inverse transformation with a near-zero scale factor.");
        }
        return z.subtract(translation).divide(scale);
    }

    public ComplexNumber getScale() {
        return scale;
    }

    public ComplexNumber getTranslation() {
        return translation;
    }

    public static Transformation compose(Transformation f1, Transformation f2) {
        ComplexNumber newScale = f2.getScale().multiply(f1.getScale());
        ComplexNumber newTranslation = f2.getScale().multiply(f1.getTranslation()).add(f2.getTranslation());
        return new Transformation(newScale, newTranslation);
    }

    public Optional<ComplexNumber> getFixedPoint() {
        ComplexNumber one = new ComplexNumber(1, 0);
        ComplexNumber denominator = one.subtract(getScale());
        if (denominator.abs() < 1e-10) {
            return Optional.empty();
        }
        return Optional.of(getTranslation().divide(denominator));
    }
}



