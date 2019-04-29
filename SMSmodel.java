package com.kalom.unipismartalert;

import java.util.ArrayList;

public class SMSmodel {

    private ArrayList<String> phoneNum;
    private String message;

    public ArrayList<String> getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(ArrayList<String> phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
