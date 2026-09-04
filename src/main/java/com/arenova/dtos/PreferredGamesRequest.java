package com.arenova.dtos;

import lombok.Data;

import java.util.List;

@Data
public class PreferredGamesRequest {
    private List<String> preferredGames;
}
