package com.intellicentrics.icme.sdk.script.service;

import com.intellicentrics.icme.sdk.script.Application;
import com.intellicentrics.icme.sdk.script.constants.UtilityConfConstants;
import com.intellicentrics.icme.sdk.script.model.ParentMapping;
import com.intellicentrics.icme.sdk.script.model.VendorSetting;
import com.intellicentrics.icme.sdk.script.utility.CommonUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLDecoder;
import java.security.CodeSource;

@Service
public class CertificateManagementService {

    @Autowired
    private ResourceLoader resourceLoader;

    public void createRootCA(ParentMapping configBean) throws IOException {
        String command;
        String opensslPath;
        String rootCAName = CommonUtil.concatWithOutputFolder(UtilityConfConstants.RootCAName, configBean);

        if (new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.RootCAName, configBean) + ".key").exists() &&
                !UtilityConfConstants.AlwaysGenerateRootCA) {
            return;
        }
        CommonUtil.printHeader(String.format("* Generating the root CA %s.key and %s.crt ", rootCAName, rootCAName));
        opensslPath = (configBean.getOpenSslPath() != null && !configBean.getOpenSslPath().isEmpty() &&
                new File(configBean.getOpenSslPath()).exists())
                ? configBean.getOpenSslPath()
                : "openssl";
        command = opensslPath + " genrsa -out " + rootCAName + ".key " + UtilityConfConstants.RootCAKeySize + " " + UtilityConfConstants.GenerateKeyFileExtraArguments;
        CommonUtil.executeCommand(command, new String[]{}, "./");
        InputStream stream = getClass().getClassLoader().getResourceAsStream("application.conf");
        File targetFile = new File("app.conf");
        FileUtils.copyInputStreamToFile(stream, targetFile);

        command = opensslPath + " req -new -x509 -days 3650 -key " + rootCAName +
                ".key -out " + rootCAName + ".crt -config " +
                targetFile.getAbsolutePath() + " " + UtilityConfConstants.GenerateCRTExtraArguments;
        CommonUtil.executeCommand(command, new String[]{}, "./");
        targetFile.delete();
    }

    // On windows install the cert into Cert:\LocalMachine\Root
    public void makeCertTrusted(ParentMapping configBean) throws IOException {
        String command;
        String rootCACert = CommonUtil.concatWithOutputFolder(UtilityConfConstants.RootCAName, configBean) + ".crt";

        if (!UtilityConfConstants.MakeCertTrusted ||
                (!System.getProperty("os.name").startsWith("Windows") &&
                        !System.getProperty("os.name").startsWith("win"))) {
            return;
        }
        CommonUtil.printHeader(String.format("* Making the certificate %s trusted ", rootCACert));
        if (System.getProperty("os.name").startsWith("Windows") || System.getProperty("os.name").startsWith("win")) {
            command = String.format("certutil -addstore \"Root\" \"%s\"", rootCACert);
            CommonUtil.executeCommand(command, new String[]{}, "./");
        }
    }

    // keytool -genkey -v -alias clientcrt -keyalg RSA -validity 3650 -keystore clientcrt.jks -storepass clientpwd -keypass clientpwd
    // keytool -certreq -alias clientcrt -file clientcrt.csr -keystore clientcrt.jks -storepass clientpwd
    public void createKeysAndCSR(ParentMapping configBean, VendorSetting setting) throws Exception {

        String command;
        String keyToolPath;
        keyToolPath = (UtilityConfConstants.KeyToolPath != null && !UtilityConfConstants.KeyToolPath.isEmpty() &&
                new File(UtilityConfConstants.KeyToolPath).exists())
                ? UtilityConfConstants.KeyToolPath
                : "keytool";
        if (!new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean)).exists() ||
                (new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean)).exists() &&
                        UtilityConfConstants.AlwaysGenerateKeyStore)) {

            CommonUtil.printHeader(String.format("* Generating the Keystore File %s", CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean)));
            if (new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean)).exists()) {
                if (!new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean)).delete()) {
                    throw new Exception("Unable to delete the old store file: " + CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean));
                }
            }
            command = String.format("%s -genkey -v -alias %s -keyalg RSA -validity %d -keystore %s -storepass %s -keypass %s %s",
                    keyToolPath, configBean.getKeystoreBean().getAlias(), UtilityConfConstants.ValidityPeriod, CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean), setting.getP12password(), setting.getP12password(),
                    UtilityConfConstants.GenerateKeyStoreExtraArguments);
            CommonUtil.executeCommand(command, new String[]{
                    configBean.getKeystoreBean().getCommonName(),
                    configBean.getKeystoreBean().getOrganizationUnit(),
                    setting.getName(),
                    configBean.getKeystoreBean().getCity(),
                    configBean.getKeystoreBean().getState(),
                    configBean.getKeystoreBean().getCountry(),
                    "yes"
            }, "./");
        }

        if (!new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreCSROutputFile, configBean)).exists() ||
                (new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreCSROutputFile, configBean)).exists() &&
                        UtilityConfConstants.AlwaysGenerateKeyStoreCSR)) {

            CommonUtil.printHeader(String.format("* Generating the Keystore CSR File %s", CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreCSROutputFile, configBean)));
            if (new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreCSROutputFile, configBean)).exists()) {
                if (!new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreCSROutputFile, configBean)).delete()) {
                    throw new Exception("Unable to delete the old store csr file: " + CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreCSROutputFile, configBean));
                }
            }
            command = String.format("%s -certreq -alias %s -file %s -keystore %s -storepass %s %s",
                    keyToolPath, configBean.getKeystoreBean().getAlias(), CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreCSROutputFile, configBean),
                    CommonUtil.concatWithOutputFolder(UtilityConfConstants.KeyStoreOutputFile, configBean), setting.getP12password(), UtilityConfConstants.GenerateCSRExtraArguments);
            CommonUtil.executeCommand(command, new String[]{}, "./");
        }
    }

    // mkdir -p demoCA/newcerts
    // touch demoCA/index.txt
    // echo '01' > demoCA/serial
    public void createDemoCAFolderAndFiles(ParentMapping configBean) throws Exception {
        CommonUtil.printHeader(String.format("* Creating the folders and files in %s", CommonUtil.concatWithOutputFolder("demoCA/newcerts", configBean)));
        if (UtilityConfConstants.AlwaysCreateFilesInDemoCA) {
            if (new File(CommonUtil.concatWithOutputFolder("demoCA/", configBean)).exists()) {
                if (!CommonUtil.deleteFolder(new File(CommonUtil.concatWithOutputFolder("demoCA/", configBean)))) {
                    throw new Exception("Unable to delete the folder: " + CommonUtil.concatWithOutputFolder("demoCA/", configBean));
                }
            }
        }
        if (!new File(CommonUtil.concatWithOutputFolder("demoCA/newcerts", configBean)).exists() ||
                !new File(CommonUtil.concatWithOutputFolder("demoCA/newcerts", configBean)).isDirectory()) {

            if (!new File(CommonUtil.concatWithOutputFolder("demoCA/newcerts", configBean)).mkdirs()) {
                throw new Exception("Unable to create the folder: " + CommonUtil.concatWithOutputFolder("demoCA/newcerts", configBean));
            }
        }
        if (!new File(CommonUtil.concatWithOutputFolder("demoCA/index.txt", configBean)).exists() ||
                !new File(CommonUtil.concatWithOutputFolder("demoCA/index.txt", configBean)).isFile()) {

            if (!new File(CommonUtil.concatWithOutputFolder("demoCA/index.txt", configBean)).createNewFile()) {
                throw new Exception("Unable to create the file: " + CommonUtil.concatWithOutputFolder("demoCA/index.txt", configBean));
            }
        }
        if (!new File(CommonUtil.concatWithOutputFolder("demoCA/serial", configBean)).exists() ||
                !new File(CommonUtil.concatWithOutputFolder("demoCA/serial", configBean)).isFile()) {

            if (!new File(CommonUtil.concatWithOutputFolder("demoCA/serial", configBean)).createNewFile()) {
                throw new Exception("Unable to create the file: " + CommonUtil.concatWithOutputFolder("demoCA/serial", configBean));
            }
            PrintWriter outFile = new PrintWriter(new FileWriter(CommonUtil.concatWithOutputFolder("demoCA/serial", configBean)));
            outFile.write("01");
            outFile.flush();
            outFile.close();
        }

    }

    // openssl ca -batch -startdate 200716080000Z -enddate 300715090000Z -keyfile rootCA.key -cert rootCA.crt -policy policy_anything -out clientcrt.crt -infiles clientcrt.csr
    public void signCSR(ParentMapping configBean) throws Exception {
        String command;
        String opensslPath;

        opensslPath = (configBean.getOpenSslPath() != null && !configBean.getOpenSslPath().isEmpty() &&
                new File(configBean.getOpenSslPath()).exists())
                ? configBean.getOpenSslPath()
                : "openssl";
        if (!new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.SignedCSROutputFile, configBean)).exists() ||
                (new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.SignedCSROutputFile, configBean)).exists() &&
                        UtilityConfConstants.AlwaysGenerateSignedCSR)) {

            CommonUtil.printHeader(String.format("* Generating the Signed CSR File %s", CommonUtil.concatWithOutputFolder(UtilityConfConstants.SignedCSROutputFile, configBean)));
            if (new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.SignedCSROutputFile, configBean)).exists()) {
                if (!new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.SignedCSROutputFile, configBean)).delete()) {
                    throw new Exception("Unable to delete the old store csr file: " + CommonUtil.concatWithOutputFolder(UtilityConfConstants.SignedCSROutputFile, configBean));
                }
            }
            command = String.format("%s ca -batch -startdate %s -enddate %s -keyfile %s -cert %s -policy %s -out %s -infiles %s %s",
                    opensslPath, configBean.getKeystoreBean().getStartDate(), configBean.getKeystoreBean().getEndDate(), UtilityConfConstants.RootCAName + ".key",
                    UtilityConfConstants.RootCAName + ".crt", UtilityConfConstants.Policy, UtilityConfConstants.SignedCSROutputFile,
                    UtilityConfConstants.KeyStoreCSROutputFile, UtilityConfConstants.SignCSRExtraArguments);
            CommonUtil.executeCommand(command, new String[]{}, UtilityConfConstants.OutputFolder);
        }
    }

    public void generateP12ForBrowser(ParentMapping configBean, VendorSetting setting) throws Exception {
        String command;
        String keyToolPath;

        keyToolPath = (UtilityConfConstants.KeyToolPath != null && !UtilityConfConstants.KeyToolPath.isEmpty() &&
                new File(UtilityConfConstants.KeyToolPath).exists())
                ? UtilityConfConstants.KeyToolPath
                : "keytool";
        if (!new File(CommonUtil.concatWithOutputFolder(setting.getP12file(), configBean)).exists() ||
                (new File(CommonUtil.concatWithOutputFolder(setting.getP12file(), configBean)).exists() &&
                        UtilityConfConstants.AlwaysGenerateSignedCSR)) {

            CommonUtil.printHeader(String.format("* Generating the P12 file %s", CommonUtil.concatWithOutputFolder(setting.getP12file(), configBean)));
            if (new File(CommonUtil.concatWithOutputFolder(setting.getP12file(), configBean)).exists()) {
                if (!new File(CommonUtil.concatWithOutputFolder(setting.getP12file(), configBean)).delete()) {
                    throw new Exception("Unable to delete the old store csr file: " + CommonUtil.concatWithOutputFolder(setting.getP12file(), configBean));
                }
            }
            command = String.format("%s -importkeystore -srckeystore %s -destkeystore %s -srcstoretype JKS -deststoretype PKCS12 -srcstorepass %s -deststorepass %s %s",
                    keyToolPath, UtilityConfConstants.KeyStoreOutputFile, setting.getP12file(), setting.getP12password(), setting.getP12password(), UtilityConfConstants.GenerateP12ExtraArguments);
            CommonUtil.executeCommand(command, new String[]{}, UtilityConfConstants.OutputFolder);
        }
    }

    public void appendP12InJar(String sdkJarFileName, ParentMapping configBean, VendorSetting setting) throws Exception {
        System.out.println("=========================================================");
        StringBuilder sb = new StringBuilder();
        String path=getJarContainingFolder(Application.class);
        for (String s : path.split("\\\\")) {
            if (s.equals("target")) {
            } else if (s.equals("classes")) {
            } else {
                sb.append(s + "/");
            }
        }
        FileUtils.copyFile(new
                File(UtilityConfConstants.OutputFolder + setting.getP12file()), new
                File(sb.toString() + setting.getP12file()));
        Process ps = Runtime.getRuntime().exec(new String[]{"jar", "uf", sdkJarFileName, setting.getP12file()});
        ps.waitFor();
        java.io.InputStream is = ps.getInputStream();
        byte b[] = new byte[is.available()];
        is.read(b, 0, b.length);
        FileUtils.forceDelete(new

                File(sb.toString() + setting.getP12file()));
        System.out.println("P12 Certifcate added succesfully in " + sdkJarFileName);
        System.out.println("=========================================================");
    }
    public void addVendoreSetting(String sdkJarFileName, File vendorSetting) throws Exception {
        System.out.println("=========================================================");
        StringBuilder sb = new StringBuilder();
        String path=getJarContainingFolder(Application.class);
        for (String s : path.split("\\\\")) {
            if (s.equals("target")) {
            } else if (s.equals("classes")) {
            } else {
                sb.append(s + "/");
            }
        }
        Process ps = Runtime.getRuntime().exec(new String[]{"jar", "uf", sdkJarFileName, UtilityConfConstants.VendorSettingOutputFile});
        ps.waitFor();
        java.io.InputStream is = ps.getInputStream();
        byte b[] = new byte[is.available()];
        is.read(b, 0, b.length);
        System.out.println("Vendor setting file added successfully in " + sdkJarFileName);
        System.out.println("=========================================================");
    }

    public static String getJarContainingFolder(Class aclass) throws Exception {
        CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();
        File jarFile;
        String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
        if (!path.contains("!")) {
            jarFile = new File(codeSource.getLocation().toURI());
        } else {
            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(jarFilePath);
        }
        return jarFile.getParentFile().getAbsolutePath();
    }
}
