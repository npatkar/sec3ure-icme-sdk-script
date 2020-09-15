package com.intellicentrics.icme.sdk.script;

import com.intellicentrics.icme.sdk.script.controller.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class Application implements ApplicationRunner {

    @Autowired
    MainController mainController;

    private static final Logger logger = LoggerFactory.getLogger(Application.class);


    public static void main(String... args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments argss) throws Exception {
        if (argss.getNonOptionArgs().size() < 3) {
            throw new Exception("Invalid length of argument simply specify the config file path\ngenkeys input.json vendor-settings.json  jarfilename to append");
        }
        mainController.performOpertion(new File(argss.getNonOptionArgs().get(0)), new File(argss.getNonOptionArgs().get(1)),argss.getNonOptionArgs().get(2));
        System.exit(-1);
    }
}
