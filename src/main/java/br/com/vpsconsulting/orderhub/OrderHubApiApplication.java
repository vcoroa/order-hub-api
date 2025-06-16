package br.com.vpsconsulting.orderhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = "br.com.vpsconsulting.orderhub")
public class OrderHubApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderHubApiApplication.class, args);
	}

}
