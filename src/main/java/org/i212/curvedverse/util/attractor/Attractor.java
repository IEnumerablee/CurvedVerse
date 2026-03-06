package org.i212.curvedverse.util.attractor;

import org.i212.curvedverse.util.ComplexNumber;

public class Attractor {
    private final ComplexNumber position;
    private final String name;

    private final double temperature;
    private final double humidity;
    private final double hostility;
    private final double pollution;
    private final int character;

    public Attractor(ComplexNumber position, String name, double temperature, double humidity, double hostility, double pollution, int character) {
        this.position = position;
        this.name = name;
        this.temperature = temperature;
        this.humidity = humidity;
        this.hostility = hostility;
        this.pollution = pollution;
        this.character = character;
    }

    public ComplexNumber getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getHostility() {
        return hostility;
    }

    public double getPollution() {
        return pollution;
    }

    public int getCharacter() {
        return character;
    }
}

