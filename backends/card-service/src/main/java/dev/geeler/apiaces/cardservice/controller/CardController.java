package dev.geeler.apiaces.cardservice.controller;

import dev.geeler.apiaces.cardservice.model.Card;
import dev.geeler.apiaces.cardservice.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
public class CardController {
    @Autowired
    private CardService cardService;

    @GetMapping("/shuffled")
    public List<UUID> getShuffledDeck() {
        return cardService.getShuffledDeck();
    }

    @GetMapping()
    public List<Card> getCards() {
        return cardService.getCards();
    }

    @GetMapping("/{id}")
    public Card getCard(@PathVariable("id") UUID id) {
        return cardService.getCard(id);
    }
}