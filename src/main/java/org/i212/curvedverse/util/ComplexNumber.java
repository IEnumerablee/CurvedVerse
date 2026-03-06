package org.i212.curvedverse.util;

import java.util.Objects;
import java.util.Locale;

public class ComplexNumber {
    private final double real;
    private final double imaginary;
    private static final double EPSILON = 1e-6;

    public ComplexNumber(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double getReal() {
        return real;
    }

    public double getImaginary() {
        return imaginary;
    }

    public ComplexNumber add(ComplexNumber other) {
        return new ComplexNumber(real + other.real, imaginary + other.imaginary);
    }

    public ComplexNumber subtract(ComplexNumber other) {
        return new ComplexNumber(real - other.real, imaginary - other.imaginary);
    }

    public ComplexNumber multiply(ComplexNumber other) {
        double r = real * other.real - imaginary * other.imaginary;
        double i = real * other.imaginary + imaginary * other.real;
        return new ComplexNumber(r, i);
    }

    public ComplexNumber divide(ComplexNumber other) {
        double denominator = other.real * other.real + other.imaginary * other.imaginary;
        if (Math.abs(denominator) < 1e-12) {
            throw new ArithmeticException("Division by zero");
        }
        double r = (real * other.real + imaginary * other.imaginary) / denominator;
        double i = (imaginary * other.real - real * other.imaginary) / denominator;
        return new ComplexNumber(r, i);
    }

    public double abs() {
        return Math.sqrt(real * real + imaginary * imaginary);
    }

    public ComplexNumber conjugate() {
        return new ComplexNumber(real, -imaginary);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplexNumber that = (ComplexNumber) o;
        return Math.abs(that.real - real) < EPSILON && Math.abs(that.imaginary - imaginary) < EPSILON;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(real / EPSILON), Math.round(imaginary / EPSILON));
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.6f_%.6f", real, imaginary);
    }

    public static ComplexNumber fromString(String str) {
        try {
            String s = str.trim();
            String[] parts = s.split("_");
            if (parts.length == 2) {
                return new ComplexNumber(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            }

            // Fallback to legacy format if needed (0.00+0.00i)
            if (s.endsWith("i")) {
                s = s.substring(0, s.length() - 1);

                int splitIndex = s.lastIndexOf('+');
                if (splitIndex <= 0) {
                    throw new IllegalArgumentException("Invalid complex number format (missing separator +): " + str);
                }

                String realPart = s.substring(0, splitIndex);
                String imagPart = s.substring(splitIndex + 1);

                return new ComplexNumber(Double.parseDouble(realPart), Double.parseDouble(imagPart));
            }

            throw new IllegalArgumentException("Invalid complex number format: " + str);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse complex number: " + str, e);
        }
    }
}
