package com.intellicentrics.icme.sdk.script.constants;

public final class UtilityConfConstants {
    private UtilityConfConstants() {
    }

    public static final boolean AlwaysGenerateRootCA = false;
    public static final boolean AlwaysGenerateKeyStore = true;
    public static final boolean AlwaysGenerateKeyStoreCSR = true;
    public static final boolean AlwaysCreateFilesInDemoCA = true;
    public static final boolean AlwaysGenerateSignedCSR = true;
    public static final boolean MakeCertTrusted = true;
    public static final String OutputFolder = "./generated/files/";
    public static final String KeyToolPath = "";
    public static final String Policy = "policy_anything";
    public static final String SignCSRExtraArguments = "";
    public static final String GenerateP12ExtraArguments = "";
    public static final String GenerateCSRExtraArguments = "";
    public static final String GenerateKeyStoreExtraArguments = "";
    public static final String GenerateCRTExtraArguments = "";
    public static final String GenerateKeyFileExtraArguments = "";
    public static final int RootCAKeySize = 2048;
    public static final int ValidityPeriod = 3650;

    public static final String KeyStoreOutputFile = "clientcrt.jks";
    public static final String KeyStoreCSROutputFile = "clientcrt.csr";
    public static final String SignedCSROutputFile = "clientcrt.crt";
    public static final String RootCAName = "rootCA";
    public static final String KeyPass = "clientpwd";
    public static final String StorePass = "clientpwd";
    public static final String P12DestStorePass = "clientpwd";
    public static final String P12OutputFile = "clientcrt.p12";
    public static final String VendorSettingOutputFile = "vendor-settings.json";


}
