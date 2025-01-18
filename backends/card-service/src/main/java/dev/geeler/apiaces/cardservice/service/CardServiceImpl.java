package dev.geeler.apiaces.cardservice.service;

import dev.geeler.apiaces.cardservice.model.Card;
import dev.geeler.apiaces.cardservice.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CardServiceImpl implements CardService {
    @Autowired
    private CardRepository cardRepository;

    @Override
    public Card getCard(UUID cardId) {
        return cardRepository.findById(cardId).orElse(null);
    }

    @Override
    public List<Card> getCards() {
        List<Card> cards = cardRepository.findAll();
        cards.sort(Comparator.comparing(Card::getCardType));
        return cards;
    }

    @Override
    public List<UUID> getShuffledDeck() {
        // TODO: Implement shuffling
        return cardRepository.findAll().stream().map(Card::getId).toList();
    }

    @Override
    public Card getRandom() {
        return cardRepository.findAll().get((int) (Math.random() * cardRepository.count()));
    }
}
