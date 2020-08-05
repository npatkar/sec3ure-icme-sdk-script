package com.intellicentrics.icme.sdk.script.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellicentrics.icme.sdk.script.constants.UtilityConfConstants;
import com.intellicentrics.icme.sdk.script.model.Api;
import com.intellicentrics.icme.sdk.script.model.ParentMapping;
import com.intellicentrics.icme.sdk.script.utility.CommonUtil;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class Wso2ApiService {


    public List<String> getAPIId(ParentMapping configBean) throws IOException {
        OkHttpClient client = getUnsafeOkHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        List<String> apiIdList = new ArrayList<>();
        List<Api> apiList = configBean.getWso2Api().getApiList();
        for (Api api : apiList) {
            if (!api.getName().equals("") || !api.getVersion().equals("")) {
                String credential = Credentials.basic(configBean.getWso2Api().getAdminUser(), configBean.getWso2Api().getAdminPassword());
                String param = "name:" + api.getName() + " version:" + api.getVersion();
                Request request = new Request.Builder()
                        .url("https://localhost:9443/api/am/publisher/v1/apis?query=" + param)
                        .method("GET", null)
                        .addHeader("Authorization", credential)
                        .build();
                Response response = client.newCall(request).execute();
                if (response != null && response.code() == 200) {
                    String jsonStringResposne = response.body().string();
                    Map<String, Object> map = mapper.readValue(jsonStringResposne, Map.class);
                    if (map != null) {
                        List<Map<String, String>> listMap = (ArrayList) map.get("list");
                        apiIdList.add(listMap.get(0).get("id"));
                    }
                }
                System.out.println();
            }
        }
        return apiIdList;
    }

    public void uploadCertificate(String apiID, ParentMapping configBean) {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println("Certifcate Upload Process started ");
        OkHttpClient client = getUnsafeOkHttpClient();
        MediaType mediaType = MediaType.parse("text/plain");
        String webserviceURL = configBean.getWso2Api().getUrl().replaceAll("\\{apiId}", apiID);
        String certPath = new File(CommonUtil.concatWithOutputFolder(UtilityConfConstants.SignedCSROutputFile,configBean)).getAbsolutePath();
        String credential = Credentials.basic(configBean.getWso2Api().getAdminUser(), configBean.getWso2Api().getAdminPassword());
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("alias", configBean.getKeystoreBean().getAlias())
                .addFormDataPart("certificate", certPath,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File(certPath)))
                .addFormDataPart("tier", configBean.getWso2Api().getTier())
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
