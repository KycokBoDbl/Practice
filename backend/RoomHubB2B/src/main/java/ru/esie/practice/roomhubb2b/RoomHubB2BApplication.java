package ru.esie.practice.roomhubb2b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoomHubB2BApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomHubB2BApplication.class, args);
    }

}
