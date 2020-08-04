package com.intellicentrics.icme.sdk.script;

/**
 * @author Arpit Khatri
 * @date 25-Jul-20 03:59 PM
 */
public class ConfigEntries {
    int RootCAKeySize;
    int ValidityPeriod;
    boolean AlwaysGenerateRootCA;
    boolean AlwaysGenerateKeyStore;
    boolean AlwaysGenerateKeyStoreCSR;
    boolean AlwaysCreateFilesInDemoCA;
    boolean AlwaysGenerateSignedCSR;
    boolean MakeCertTrusted;
    String RootCAName;
    String OutputFolder;
    String OpenSSlPath;
    String ConfigFilePath;
    String KeyToolPath;
    String KeyAlias;
    String KeyStoreOutputFile;
    String KeyStoreCSROutputFile;
    String KeyPass;
    String StorePass;
    String SignedCSROutputFile;
    String KeyStore_CN;
    String KeyStore_OU;
    String KeyStore_O;
    String KeyStore_L;
    String KeyStore_ST;
    String KeyStore_C;
    String StartDate;
    String EndDate;
    String Policy;
    String P12OutputFile;
    String P12DestStorePass;
    String SignCSRExtraArguments;
    String GenerateP12ExtraArguments;
    String GenerateCSRExtraArguments;
    String GenerateKeyStoreExtraArguments;
    String GenerateCRTExtraArguments;
    String GenerateKeyFileExtraArguments;
    String wso2UploadUrl;
    String apiId;
    String tier;
    String wso2ServerUserName;
    String wso2ServerPassword;
    String SDKRepoPath;
}
