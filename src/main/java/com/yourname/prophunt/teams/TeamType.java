package com.yourname.prophunt.teams;

public enum TeamType {
    PROPS("Props", "§a"),
    HUNTERS("Hunters", "§c");

    private final String name;
    private final String color;

    TeamType(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getDisplayName() {
        return color + name;
    }
}
