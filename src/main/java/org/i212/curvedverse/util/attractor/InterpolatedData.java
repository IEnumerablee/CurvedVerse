package org.i212.curvedverse.util.attractor;

import java.util.List;

public class InterpolatedData {
    private final double temperature;
    private final double humidity;
    private final double hostility;
    private final double pollution;
    private final List<Integer> characters;

    public InterpolatedData(double temperature, double humidity, double hostility, double pollution, List<Integer> characters) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.hostility = hostility;
        this.pollution = pollution;
        this.characters = characters;
    }

    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getHostility() { return hostility; }
    public double getPollution() { return pollution; }
    public List<Integer> getCharacters() { return characters; }
}

