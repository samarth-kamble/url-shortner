package com.shortlink;

import org.springframework.boot.SpringApplication;

public class TestShortlinkApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShortlinkApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
