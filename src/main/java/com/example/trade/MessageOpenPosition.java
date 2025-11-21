package com.example.trade;

public class MessageOpenPosition {
    public String getInstrument() {
        return instrument;
    }

    public int getUnits() {
        return units;
    }

    private String instrument;

    public void setUnits(int units) {
        this.units = units;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    private int units;
}
