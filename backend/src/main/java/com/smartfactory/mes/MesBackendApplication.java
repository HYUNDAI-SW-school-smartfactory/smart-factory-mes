package com.smartfactory.mes;

import com.smartfactory.mes.simulation.config.SimulationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(SimulationProperties.class)
@SpringBootApplication
public class MesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MesBackendApplication.class, args);
	}

}
