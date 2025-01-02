package dev.geeler.apiaces.gameservice.controller;

import dev.geeler.apiaces.gameservice.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka")
public class KafkaController {
    @Autowired
    private KafkaProducerService kafkaProducer;

    @GetMapping("/send/{message}")
    public String sendMessage(@PathVariable String message) {
        kafkaProducer.sendMessageDefault(message);
        return "Sent!";
    }
}
