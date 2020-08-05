package com.intellicentrics.icme.sdk.script.utility;

import com.intellicentrics.icme.sdk.script.model.ParentMapping;

import java.io.*;

public class CommonUtil {

    public static String concatWithOutputFolder(String fileName, ParentMapping configBean) {
        return configBean.getOutputFolder() + File.separator + fileName;
    }

    public static void printHeader(String header) {
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
    public static void executeCommand(String command, String[] inputs, String workingDir) throws IOException {
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
    public static boolean deleteFolder(File folder) {
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
}
