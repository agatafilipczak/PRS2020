package prs.project;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;

@SpringBootApplication

public class Spring_AF_Application {

    public static void main(String[] args) {
        SpringApplication.run(Spring_AF_Application.class, args);
    }

    public class CustomObjectMapper extends ObjectMapper {
        @PostConstruct
        public void customConfiguration() {
            // Uses Enum.toString() for serialization of an Enum
            this.enable(WRITE_ENUMS_USING_TO_STRING);
            // Uses Enum.toString() for deserialization of an Enum
            this.enable(READ_ENUMS_USING_TO_STRING);
        }
    }
}
