package com.korniienko.kalah.dao;

import com.korniienko.kalah.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
