package fr.loot1.catsandmice;

import java.util.UUID;

public class Click {

    private final UUID uuid;

    private final long date;

    private final int score;

    public Click(UUID uuid, long date, int score) {
        this.uuid = uuid;
        this.date = date;
        this.score = score;
    }

    public UUID getUUID() { return uuid; }

    public long getDate() { return date; }

    public int getScore() { return score; }

}