package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

@SpringBootApplication
@EnableHystrixDashboard
public class AppmasterApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(AppmasterApplication.class, args);
	}

}
