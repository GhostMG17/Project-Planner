package uz.mukhammad.projectplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ProjectPlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectPlannerApplication.class, args);
	}

}
