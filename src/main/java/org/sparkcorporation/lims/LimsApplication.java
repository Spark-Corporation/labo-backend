package org.sparkcorporation.lims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(systemName = "LIMS")
public class LimsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LimsApplication.class, args);
    }
}
