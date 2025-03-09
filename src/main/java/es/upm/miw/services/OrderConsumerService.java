package es.upm.miw.services;


import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OrderConsumerService {

    public void processMessage(String key, String message) {
        log.info("Processing message with key: {}, value: {}", key, message);
    }
}
