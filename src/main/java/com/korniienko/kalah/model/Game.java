package com.korniienko.kalah.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Pit.Part turn = Pit.Part.randomPart();

    private Pit.Part winner = null;

    private boolean draw;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Pit> pits = new ArrayList<>();

    private boolean gameOver;

    public void switchTurn() {
        setTurn(getTurn().opposite());
    }
}
