package com.example.trade;

public class MessageHistPrice {
    public String getInstrument() {
        return instrument;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    private String instrument;
    private String granularity;
}