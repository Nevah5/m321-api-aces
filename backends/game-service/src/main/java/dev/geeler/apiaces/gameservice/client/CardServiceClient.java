package dev.geeler.apiaces.gameservice.client;

import dev.geeler.apiaces.gameservice.model.card.Card;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "card-service")
public interface CardServiceClient {
    @GetMapping("/cards")
    List<Card> getCards();
}