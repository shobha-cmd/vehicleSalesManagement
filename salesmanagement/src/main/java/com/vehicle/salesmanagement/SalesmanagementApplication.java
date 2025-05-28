package com.vehicle.salesmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.vehicle.salesmanagement")  // Ensure the package is scanned
public class SalesmanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalesmanagementApplication.class, args);
	}
}
