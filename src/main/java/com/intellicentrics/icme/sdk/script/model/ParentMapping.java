package com.intellicentrics.icme.sdk.script.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParentMapping {

    private File outputFolder;
    private String configFilePath;
    private String openSslPath;
    @JsonProperty("keystore")
    private KeystoreBean keystoreBean;
    @JsonProperty("wso2")
    private Wso2Api wso2Api;

    public String getOpenSslPath() {
        return openSslPath;
    }

    public void setOpenSslPath(String openSslPath) {
        this.openSslPath = openSslPath;
    }

    public KeystoreBean getKeystoreBean() {
        return keystoreBean;
    }

    public void setKeystoreBean(KeystoreBean keystoreBean) {
        this.keystoreBean = keystoreBean;
    }

    public Wso2Api getWso2Api() {
        return wso2Api;
    }

    public void setWso2Api(Wso2Api wso2Api) {
        this.wso2Api = wso2Api;
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }
}


