package dev.geeler.apiaces.cardservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity(name = "card")
@Table(name = "cards")
public class Card {
    @Id
    @Getter
    private UUID id;

    @Getter
    private String name;

    @Getter
    private String description;

    @Getter
    private String image;

    @Getter
    private CardType cardType;

    @Getter
    private Integer amountInDeck;

    // builder is not needed, but if it is, set to public
    private static class Builder {
        private final Card card;

        public Builder() {
            card = new Card();
            card.id = UUID.randomUUID();
        }

        public void setName(String name) {
            card.name = name;
        }

        public void setDescription(String description) {
            card.description = description;
        }

        public void setImage(String image) {
            card.image = image;
        }

        public void setCardType(CardType cardType) {
            card.cardType = cardType;
        }

        public void setAmountInDeck(Integer amountInDeck) {
            card.amountInDeck = amountInDeck;
        }

        public Card build() {
            return card;
        }
    }
}