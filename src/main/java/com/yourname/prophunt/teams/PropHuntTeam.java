package com.yourname.prophunt.teams;

import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PropHuntTeam {
    private final TeamType type;
    private final List<UUID> members = new ArrayList<>();
    private int score = 0;

    public PropHuntTeam(TeamType type) {
        this.type = type;
    }

    public void addMember(ServerPlayer player) {
        if (!members.contains(player.getUUID())) {
            members.add(player.getUUID());
        }
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }

    public TeamType getType() {
        return type;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int amount) {
        this.score += amount;
    }

    public void resetScore() {
        this.score = 0;
    }

    public int getMemberCount() {
        return members.size();
    }
}
