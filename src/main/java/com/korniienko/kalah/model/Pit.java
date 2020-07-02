package com.korniienko.kalah.model;

import lombok.*;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Data
@NoArgsConstructor
@Entity
public class Pit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Game game;

    private Integer index;

    private boolean isKalah;

    private Part part;

    private Integer weight;

    public Pit(Game game, Integer index, boolean isKalah, Part part, Integer weight) {
        this.game = game;
        this.index = index;
        this.isKalah = isKalah;
        this.part = part;
        this.weight = weight;
    }

    public enum Part {
        NORTH, SOUTH;

        private static final List<Part> VALUES =
                Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();

        public static Part randomPart() {
            final int randomIndex = RANDOM.nextInt(SIZE);
            return VALUES.get(randomIndex);
        }

        public Part opposite() {
            return this == SOUTH ? NORTH : SOUTH;
        }
    }
}
