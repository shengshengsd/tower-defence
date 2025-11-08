package ch.logixisland.anuto.business.game;

import java.util.Date;

public class LeaderboardEntry {
    private final String mMapId;
    private final int mScore;
    private final Date mDate;
    private final int mWave;
    private final int mLives;

    public LeaderboardEntry(String mapId, int score, Date date, int wave, int lives) {
        mMapId = mapId;
        mScore = score;
        mDate = date;
        mWave = wave;
        mLives = lives;
    }

    public String getMapId() { return mMapId; }
    public int getScore() { return mScore; }
    public Date getDate() { return mDate; }
    public int getWave() { return mWave; }
    public int getLives() { return mLives; }
}
