package com.genkeys;

import io.github.thecarisma.Konfiger;
import io.github.thecarisma.KonfigerStream;
import okhttp3.*;
import org.apache.commons.io.FileUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * @author Arpit Khatri
 * @date 25-Jul-20 03:55 PM
 */
public class Main {

    ConfigEntries config = new ConfigEntries();
    File outputFolder;

    public Main(File inputFile, String sdkJarFileName) throws Exception {
        if (!inputFile.exists()) {
            throw new FileNotFoundException("The file '" + inputFile.getAbsolutePath() + "' does not exist");
        }
        KonfigerStream stream = new KonfigerStream(inputFile);
        stream.setCommentPrefix("[");
        Konfiger konfiger = new Konfiger(stream);
        konfiger.resolve(config);
        config.ConfigFilePath = inputFile.getAbsolutePath();

        outputFolder = new File(config.OutputFolder);
        if (!outputFolder.exists() || !outputFolder.isDirectory()) {
            if (!outputFolder.mkdirs()) {
                throw new FileNotFoundException("The output folder " + outputFolder.getAbsolutePath() + " does not exist");
            }
        }
        createRootCA();
        makeCertTrusted();
        createKeysAndCSR();
        createDemoCAFolderAndFiles();
        signCSR();
        generateP12ForBrowser();
        uploadCertificate();
        copyP12Files();
        appendP12InJar(sdkJarFileName);
    }
    public static String getJarContainingFolder(Class aclass) throws Exception {
        CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();
        File jarFile;
        if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
        }
        else {
            String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(jarFilePath);
        }
        return jarFile.getParentFile().getAbsolutePath();
    }
    private void appendP12InJar(String sdkJarFileName) throws Exception {
        System.out.println("=========================================================");
        String path = getJarContainingFolder(Main.class);
        System.out.println(path);
        StringBuilder sb = new StringBuilder();
        for (String s : path.split("\\\\")) {
            if (s.equals("target")) {
            }else if( s.equals("classes")) {
            } else {
                sb.append(s + "/");
            }
        }
        FileUtils.copyFile(new File(config.OutputFolder + config.P12OutputFile), new File(sb.toString() + config.P12OutputFile));
        Process ps = Runtime.getRuntime().exec(new String[]{"jar", "uf", sdkJarFileName, config.P12OutputFile});
        ps.waitFor();
        java.io.InputStream is = ps.getInputStream();
        byte b[] = new byte[is.available()];
        is.read(b, 0, b.length);
        FileUtils.forceDelete(new File(sb.toString() + config.P12OutputFile));
        System.out.println("P12 Certifcate added succesfully in "+sdkJarFileName);
        System.out.println("=========================================================");
    }

    private void copyP12Files() throws IOException {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println("P12 file Copy to Destination " + config.SDKRepoPath);
        if (new File(config.SDKRepoPath).exists()) {
            FileUtils.copyFile(new File(config.OutputFolder + config.P12OutputFile), new File(config.SDKRepoPath + config.P12OutputFile));
            System.out.println(config.P12OutputFile + " File Copied succesfully ." + config.SDKRepoPath + config.P12OutputFile);
        } else {
            System.out.println("Destination path is not available to copy." + config.P12OutputFile);
        }
        System.out.println("=========================================================");
    }

    private void uploadCertificate() {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println("Certifcate Upload Process started ");
        OkHttpClient client = getUnsafeOkHttpClient();
        MediaType mediaType = MediaType.parse("text/plain");
        String webserviceURL = config.wso2UploadUrl.replaceAll("\\{apiId}", config.apiId);
        String certPath = new File(concatWithOutputFolder(config.SignedCSROutputFile)).getAbsolutePath();
        String credential = Credentials.basic(config.wso2ServerUserName, config.wso2ServerPassword);
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("alias", config.KeyAlias)
                .addFormDataPart("certificate", certPath,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File(certPath)))
                .addFormDataPart("tier", config.tier)
                .build();
        Request request = new Request.Builder()
                .url(webserviceURL)
                .method("POST", body)
                .addHeader("Authorization", credential)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.code() == 201) {
                System.out.println(" Certificate Upload Sucessfully .. " + response);
            } else {
                System.out.println(" Certifcate Upload Failed :: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("=========================================================");
    }

    // openssl genrsa -out rootCA.key 2048
    private void createRootCA() throws IOException, InterruptedException {
        String command;
        String opensslPath;
        String rootCAName = concatWithOutputFolder(config.RootCAName);

        if (new File(concatWithOutputFolder(config.RootCAName) + ".key").exists() &&
                !config.AlwaysGenerateRootCA) {
            return;
        }
        printHeader(String.format("* Generating the root CA %s.key and %s.crt ", rootCAName, rootCAName));
        opensslPath = (config.OpenSSlPath != null && !config.OpenSSlPath.isEmpty() &&
                new File(config.OpenSSlPath).exists())
                ? config.OpenSSlPath
                : "openssl";
        command = opensslPath + " genrsa -out " + rootCAName + ".key " + config.RootCAKeySize + " " + config.GenerateKeyFileExtraArguments;
        executeCommand(command, new String[]{}, "./");

        command = opensslPath + " req -new -x509 -days 3650 -key " + rootCAName +
                ".key -out " + rootCAName + ".crt -config " +
                config.ConfigFilePath + " " + config.GenerateCRTExtraArguments;
        executeCommand(command, new String[]{}, "./");
    }

    // On windows install the cert into Cert:\LocalMachine\Root
    private void makeCertTrusted() throws IOException {
        String command;
        String rootCACert = concatWithOutputFolder(config.RootCAName) + ".crt";

        if (!config.MakeCertTrusted ||
                (!System.getProperty("os.name").startsWith("Windows") &&
                        !System.getProperty("os.name").startsWith("win"))) {
            return;
        }
        printHeader(String.format("* Making the certificate %s trusted ", rootCACert));
        if (System.getProperty("os.name").startsWith("Windows") || System.getProperty("os.name").startsWith("win")) {
            command = String.format("certutil -addstore \"Root\" \"%s\"", rootCACert);
            executeCommand(command, new String[]{}, "./");
        }
    }

    // keytool -genkey -v -alias clientcrt -keyalg RSA -validity 3650 -keystore clientcrt.jks -storepass clientpwd -keypass clientpwd
    // keytool -certreq -alias clientcrt -file clientcrt.csr -keystore clientcrt.jks -storepass clientpwd
    private void createKeysAndCSR() throws Exception {
        String command;
        String keyToolPath;

        keyToolPath = (config.KeyToolPath != null && !config.KeyToolPath.isEmpty() &&
                new File(config.KeyToolPath).exists())
                ? config.KeyToolPath
                : "keytool";
        if (!new File(concatWithOutputFolder(config.KeyStoreOutputFile)).exists() ||
                (new File(concatWithOutputFolder(config.KeyStoreOutputFile)).exists() &&
                        config.AlwaysGenerateKeyStore)) {

            printHeader(String.format("* Generating the Keystore File %s", concatWithOutputFolder(config.KeyStoreOutputFile)));
            if (new File(concatWithOutputFolder(config.KeyStoreOutputFile)).exists()) {
                if (!new File(concatWithOutputFolder(config.KeyStoreOutputFile)).delete()) {
                    throw new Exception("Unable to delete the old store file: " + concatWithOutputFolder(config.KeyStoreOutputFile));
                }
            }
            command = String.format("%s -genkey -v -alias %s -keyalg RSA -validity %d -keystore %s -storepass %s -keypass %s %s",
                    keyToolPath, config.KeyAlias, config.ValidityPeriod, concatWithOutputFolder(config.KeyStoreOutputFile), config.StorePass, config.KeyPass,
                    config.GenerateKeyStoreExtraArguments);
            executeCommand(command, new String[]{
                    config.KeyStore_CN,
                    config.KeyStore_OU,
                    config.KeyStore_O,
                    config.KeyStore_L,
                    config.KeyStore_ST,
                    config.KeyStore_C,
                    "yes"
            }, "./");
        }

        if (!new File(concatWithOutputFolder(config.KeyStoreCSROutputFile)).exists() ||
                (new File(concatWithOutputFolder(config.KeyStoreCSROutputFile)).exists() &&
                        config.AlwaysGenerateKeyStoreCSR)) {

            printHeader(String.format("* Generating the Keystore CSR File %s", concatWithOutputFolder(config.KeyStoreCSROutputFile)));
            if (new File(concatWithOutputFolder(config.KeyStoreCSROutputFile)).exists()) {
                if (!new File(concatWithOutputFolder(config.KeyStoreCSROutputFile)).delete()) {
                    throw new Exception("Unable to delete the old store csr file: " + concatWithOutputFolder(config.KeyStoreCSROutputFile));
                }
            }
            command = String.format("%s -certreq -alias %s -file %s -keystore %s -storepass %s %s",
                    keyToolPath, config.KeyAlias, concatWithOutputFolder(config.KeyStoreCSROutputFile),
                    concatWithOutputFolder(config.KeyStoreOutputFile), config.StorePass, config.GenerateCSRExtraArguments);
            executeCommand(command, new String[]{}, "./");
        }
    }

    // mkdir -p demoCA/newcerts
    // touch demoCA/index.txt
    // echo '01' > demoCA/serial
    private void createDemoCAFolderAndFiles() throws Exception {
        printHeader(String.format("* Creating the folders and files in %s", concatWithOutputFolder("demoCA/newcerts")));
        if (config.AlwaysCreateFilesInDemoCA) {
            if (new File(concatWithOutputFolder("demoCA/")).exists()) {
                if (!deleteFolder(new File(concatWithOutputFolder("demoCA/")))) {
                    throw new Exception("Unable to delete the folder: " + concatWithOutputFolder("demoCA/"));
                }
            }
        }
        if (!new File(concatWithOutputFolder("demoCA/newcerts")).exists() ||
                !new File(concatWithOutputFolder("demoCA/newcerts")).isDirectory()) {

            if (!new File(concatWithOutputFolder("demoCA/newcerts")).mkdirs()) {
                throw new Exception("Unable to create the folder: " + concatWithOutputFolder("demoCA/newcerts"));
            }
        }
        if (!new File(concatWithOutputFolder("demoCA/index.txt")).exists() ||
                !new File(concatWithOutputFolder("demoCA/index.txt")).isFile()) {

            if (!new File(concatWithOutputFolder("demoCA/index.txt")).createNewFile()) {
                throw new Exception("Unable to create the file: " + concatWithOutputFolder("demoCA/index.txt"));
            }
        }
        if (!new File(concatWithOutputFolder("demoCA/serial")).exists() ||
                !new File(concatWithOutputFolder("demoCA/serial")).isFile()) {

            if (!new File(concatWithOutputFolder("demoCA/serial")).createNewFile()) {
                throw new Exception("Unable to create the file: " + concatWithOutputFolder("demoCA/serial"));
            }
            PrintWriter outFile = new PrintWriter(new FileWriter(concatWithOutputFolder("demoCA/serial")));
            outFile.write("01");
            outFile.flush();
            outFile.close();
        }

    }

    // openssl ca -batch -startdate 200716080000Z -enddate 300715090000Z -keyfile rootCA.key -cert rootCA.crt -policy policy_anything -out clientcrt.crt -infiles clientcrt.csr
    private void signCSR() throws Exception {
        String command;
        String opensslPath;

        opensslPath = (config.OpenSSlPath != null && !config.OpenSSlPath.isEmpty() &&
                new File(config.OpenSSlPath).exists())
                ? config.OpenSSlPath
                : "openssl";
        if (!new File(concatWithOutputFolder(config.SignedCSROutputFile)).exists() ||
                (new File(concatWithOutputFolder(config.SignedCSROutputFile)).exists() &&
                        config.AlwaysGenerateSignedCSR)) {

            printHeader(String.format("* Generating the Signed CSR File %s", concatWithOutputFolder(config.SignedCSROutputFile)));
            if (new File(concatWithOutputFolder(config.SignedCSROutputFile)).exists()) {
                if (!new File(concatWithOutputFolder(config.SignedCSROutputFile)).delete()) {
                    throw new Exception("Unable to delete the old store csr file: " + concatWithOutputFolder(config.SignedCSROutputFile));
                }
            }
            command = String.format("%s ca -batch -startdate %s -enddate %s -keyfile %s -cert %s -policy %s -out %s -infiles %s %s",
                    opensslPath, config.StartDate, config.EndDate, config.RootCAName + ".key",
                    config.RootCAName + ".crt", config.Policy, config.SignedCSROutputFile,
                    config.KeyStoreCSROutputFile, config.SignCSRExtraArguments);
            executeCommand(command, new String[]{}, config.OutputFolder);
        }
    }

    private void generateP12ForBrowser() throws Exception {
        String command;
        String keyToolPath;

        keyToolPath = (config.KeyToolPath != null && !config.KeyToolPath.isEmpty() &&
                new File(config.KeyToolPath).exists())
                ? config.KeyToolPath
                : "keytool";
        if (!new File(concatWithOutputFolder(config.P12OutputFile)).exists() ||
                (new File(concatWithOutputFolder(config.P12OutputFile)).exists() &&
                        config.AlwaysGenerateSignedCSR)) {

            printHeader(String.format("* Generating the P12 file %s", concatWithOutputFolder(config.P12OutputFile)));
            if (new File(concatWithOutputFolder(config.P12OutputFile)).exists()) {
                if (!new File(concatWithOutputFolder(config.P12OutputFile)).delete()) {
                    throw new Exception("Unable to delete the old store csr file: " + concatWithOutputFolder(config.P12OutputFile));
                }
            }
            command = String.format("%s -importkeystore -srckeystore %s -destkeystore %s -srcstoretype JKS -deststoretype PKCS12 -srcstorepass %s -deststorepass %s %s",
                    keyToolPath, config.KeyStoreOutputFile, config.P12OutputFile, config.StorePass, config.P12DestStorePass, config.GenerateP12ExtraArguments);
            executeCommand(command, new String[]{}, config.OutputFolder);
        }
    }

    private String concatWithOutputFolder(String fileName) {
        return outputFolder + File.separator + fileName;
    }

    private void printHeader(String header) {
        int i = header.length();
        StringBuilder underline = new StringBuilder("===");
        while (--i > 0) {
            underline.append('=');
        }
        System.out.println();
        System.out.println(underline);
        System.out.println(header);
        System.out.println(underline);
    }

    private void executeCommand(String command, String[] inputs, String workingDir) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        builder.directory(new File(workingDir).getAbsoluteFile());
        Process process = builder.start();

        OutputStream stdin = process.getOutputStream();
        InputStream stdout = process.getInputStream();
        InputStream stderr = process.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
        BufferedReader error = new BufferedReader(new InputStreamReader(stderr));

        for (String input : inputs) {
            writer.write(input + "\n"); // Don't forget the '\n' here, otherwise it'll continue to wait for input
            writer.flush();
        }

        String line;
        while ((line = reader.readLine()) != null) System.out.println(line);
        while ((line = error.readLine()) != null) System.out.println(line);
        writer.close();
    }

    public boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    if (!deleteFolder(f)) {
                        return false;
                    }
                } else {
                    if (!f.delete()) {
                        return false;
                    }
                }
            }
        }
        return folder.delete();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("Invalid length of argument simply specify the config file path\ngenkeys input.conf  jarfilename to append");
        }
        new Main(new File(args[0]), args[1]);
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {

                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws
                                CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws
                                CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);

            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
