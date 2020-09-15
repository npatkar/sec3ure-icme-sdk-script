package com.intellicentrics.icme.sdk.script.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorSetting {
    private String name;
    private String p12file;
    private String p12password;
    private String region;

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public String getP12file(){
        return p12file;
    }
    public void setP12file(String p12file){
        this.p12file = p12file;
    }
    public String getP12password(){
        return p12password;
    }
    public void setP12password(String p12password){
        this.p12password = p12password;
    }
    public String getRegion(){
        return p12password;
    }
    public void setRegion(String region){
        this.region = region;
    }
}