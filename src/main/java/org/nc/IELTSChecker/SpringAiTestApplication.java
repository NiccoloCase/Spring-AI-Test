package org.nc.IELTSChecker;

import org.nc.IELTSChecker.services.CsvIeltsTask2Loader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringAiTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiTestApplication.class, args);
    }

    @Bean
    CommandLineRunner loadData(CsvIeltsTask2Loader csvIeltsTask2Loader) {
        return args -> {
            try {
                csvIeltsTask2Loader.loadCsvEssays();
                System.out.println("IELTS Dataset data loaded successfully");
            } catch (Exception e) {
                System.err.println("Failed to load IELTS essay data: " + e.getMessage());
            }
        };
    }
}