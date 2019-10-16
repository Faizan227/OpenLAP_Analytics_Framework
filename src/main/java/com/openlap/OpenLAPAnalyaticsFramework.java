package com.openlap;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Application of the OpenLAP-Core
 */
@SpringBootApplication
@ComponentScan(basePackageClasses = { OpenLAPAnalyaticsFramework.class })
public class OpenLAPAnalyaticsFramework {


    public static String API_VERSION_NUMBER;

    /**
     * Start the application
     *
     * @paramargs
     */
    public static void main(String[] args) {
        SpringApplication.run(OpenLAPAnalyaticsFramework.class, args);
    }
}
