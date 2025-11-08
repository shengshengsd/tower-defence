package ch.logixisland.anuto.business.game;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.logixisland.anuto.business.tower.TowerSelector;
import ch.logixisland.anuto.business.wave.WaveManager;
import ch.logixisland.anuto.engine.logic.persistence.Persister;
import ch.logixisland.anuto.util.container.KeyValueStore;

public class GameState implements ScoreBoard.Listener, Persister {

    public interface Listener {
        void gameRestart();

        void gameOver();
    }

    private final ScoreBoard mScoreBoard;
    private final HighScores mHighScores;
    private final TowerSelector mTowerSelector;
    private final LeaderboardRepository mLeaderboardRepository;
    private final GameLoader mGameLoader;
    private WaveManager mWaveManager; // 改为非final，通过setter设置

    private boolean mGameOver = false;
    private boolean mGameStarted = false;
    private int mFinalScore = 0;

    private final List<Listener> mListeners = new CopyOnWriteArrayList<>();

    public GameState(ScoreBoard scoreBoard, HighScores highScores, TowerSelector towerSelector,
                     LeaderboardRepository leaderboardRepository, GameLoader gameLoader) {
        mScoreBoard = scoreBoard;
        mHighScores = highScores;
        mTowerSelector = towerSelector;
        mLeaderboardRepository = leaderboardRepository;
        mGameLoader = gameLoader;

        mScoreBoard.addListener(this);
    }

    // 新增：设置WaveManager的方法
    public void setWaveManager(WaveManager waveManager) {
        mWaveManager = waveManager;
    }

    public boolean isGameOver() {
        return mGameOver;
    }

    public boolean isGameStarted() {
        return !mGameOver && mGameStarted;
    }

    public int getFinalScore() {
        return mFinalScore;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    public void gameStarted() {
        mGameStarted = true;
    }

    @Override
    public void livesChanged(int lives) {
        if (!mGameOver && mScoreBoard.getLives() < 0) {
            setGameOver(true);
        }
    }

    @Override
    public void creditsChanged(int credits) {

    }

    @Override
    public void bonusChanged(int waveBonus, int earlyBonus) {

    }

    @Override
    public void resetState() {
        setGameOver(false);
        mGameStarted = false;
    }

    @Override
    public void writeState(KeyValueStore gameState) {
        gameState.putInt("finalScore", mFinalScore);
    }

    @Override
    public void readState(KeyValueStore gameState) {
        setGameOver(gameState.getInt("lives") < 0);
        mGameStarted = gameState.getInt("waveNumber") > 0;
        mFinalScore = gameState.getInt("finalScore");
    }

    private void setGameOver(boolean gameOver) {
        mGameOver = gameOver;

        if (gameOver) {
            mHighScores.updateHighScore();
            mFinalScore = mScoreBoard.getScore();
            mTowerSelector.setControlsEnabled(false);

            // 新增：记录到排行榜
            String currentMapId = mGameLoader.getCurrentMapId();
            mLeaderboardRepository.addEntry(
                    currentMapId,
                    mFinalScore,
                    getCurrentWaveNumber(),
                    mScoreBoard.getLives()
            );

            for (Listener listener : mListeners) {
                listener.gameOver();
            }
        }

        if (!gameOver) {
            mGameStarted = false;
            mFinalScore = 0;
            mTowerSelector.setControlsEnabled(true);

            for (Listener listener : mListeners) {
                listener.gameRestart();
            }
        }
    }

    private int getCurrentWaveNumber() {
        // 通过 WaveManager 获取当前波数
        return mWaveManager != null ? mWaveManager.getWaveNumber() : 0;
    }
}
