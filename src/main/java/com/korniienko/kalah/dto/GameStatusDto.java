package com.korniienko.kalah.dto;

import lombok.Value;

import java.util.Map;

@Value
public class GameStatusDto {
    Map<Integer, String> status;
}
