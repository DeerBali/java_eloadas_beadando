package com.example.bank;

public class MessagePrice {


    private String currency;
    private String startDate;
    private String endDate;

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getCurrency() {
        return currency;
    }


}
