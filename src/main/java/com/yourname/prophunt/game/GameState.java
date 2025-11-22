package com.yourname.prophunt.game;

public enum GameState {
    LOBBY,      // Lobby with 60s countdown to join
    STARTING,   // Game is about to start (assign roles)
    HIDING,     // Props are hiding, hunters are blind
    PLAYING,    // Active hunting phase
    ENDING,     // Game is ending
    FINISHED    // Game has ended
}
