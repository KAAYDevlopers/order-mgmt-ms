package com.abw12.absolutefitness.ordermgmtms;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@OpenAPIDefinition(info = @Info(title = "order-mgmt-ms",
		description = "Handle order placement process and maintain order history for a user",
		version = "3.0"))
@EnableTransactionManagement
@EnableFeignClients(basePackages = "com.abw12.absolutefitness.ordermgmtms.gateway.interfaces")
public class OrderMgmtMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderMgmtMsApplication.class, args);
	}

}
