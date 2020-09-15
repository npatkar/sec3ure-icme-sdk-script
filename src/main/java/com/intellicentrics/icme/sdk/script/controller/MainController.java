package com.intellicentrics.icme.sdk.script.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellicentrics.icme.sdk.script.constants.UtilityConfConstants;
import com.intellicentrics.icme.sdk.script.model.ParentMapping;
import com.intellicentrics.icme.sdk.script.model.VendorSetting;
import com.intellicentrics.icme.sdk.script.service.CertificateManagementService;
import com.intellicentrics.icme.sdk.script.service.Wso2ApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

@RestController
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    CertificateManagementService certificateManagementService;

    @Autowired
    Wso2ApiService wso2ApiService;

    public void performOpertion(File inputFile,File settingFile, String sdkJarFileName) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        if (!inputFile.exists()) {
            throw new FileNotFoundException("The file '" + inputFile.getAbsolutePath() + "' does not exist");
        }
        if (!settingFile.exists()) {
            throw new FileNotFoundException("The file '" + settingFile.getAbsolutePath() + "' does not exist");
        }
        ParentMapping configBean = objectMapper.readValue(inputFile, ParentMapping.class);
        VendorSetting setting = objectMapper.readValue(settingFile, VendorSetting.class);

        configBean.setConfigFilePath(inputFile.getAbsolutePath());

        configBean.setOutputFolder(new File(UtilityConfConstants.OutputFolder));

        if (!configBean.getOutputFolder().exists() || !configBean.getOutputFolder().isDirectory()) {
            if (!configBean.getOutputFolder().mkdirs()) {
                throw new FileNotFoundException("The output folder " + configBean.getOutputFolder().getAbsolutePath() + " does not exist");
            }
        }

        certificateManagementService.createRootCA(configBean);
        certificateManagementService.makeCertTrusted(configBean);
        certificateManagementService.createKeysAndCSR(configBean,setting);
        certificateManagementService.createDemoCAFolderAndFiles(configBean);
        certificateManagementService.signCSR(configBean);
        certificateManagementService.generateP12ForBrowser(configBean,setting);
        List<String> apiIdList = wso2ApiService.getAPIId(configBean);
        apiIdList.forEach(s -> {
            wso2ApiService.uploadCertificate(s, configBean);
        });
        certificateManagementService.appendP12InJar(sdkJarFileName, configBean, setting);
        certificateManagementService.addVendoreSetting(sdkJarFileName,settingFile);
    }
}
