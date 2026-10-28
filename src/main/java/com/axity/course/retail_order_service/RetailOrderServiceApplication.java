package com.axity.course.retail_order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.axity.course.retailorderservice"
})
public class RetailOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RetailOrderServiceApplication.class, args);
	}

}
