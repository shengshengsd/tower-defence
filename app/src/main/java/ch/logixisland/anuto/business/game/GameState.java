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
    private final CoinManager mCoinManager; // 新增
    private WaveManager mWaveManager;

    private boolean mGameOver = false;
    private boolean mGameStarted = false;
    private int mFinalScore = 0;

    private final List<Listener> mListeners = new CopyOnWriteArrayList<>();

    // 修改构造函数
    public GameState(ScoreBoard scoreBoard, HighScores highScores, TowerSelector towerSelector,
                     LeaderboardRepository leaderboardRepository, GameLoader gameLoader,
                     CoinManager coinManager) { // 新增参数
        mScoreBoard = scoreBoard;
        mHighScores = highScores;
        mTowerSelector = towerSelector;
        mLeaderboardRepository = leaderboardRepository;
        mGameLoader = gameLoader;
        mCoinManager = coinManager; // 新增

        mScoreBoard.addListener(this);
    }

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

            // 新增：游戏结束时奖励coin
            int coinsEarned = calculateCoinsFromScore(mFinalScore);
            if (coinsEarned > 0) {
                mCoinManager.addCoins(coinsEarned);
            }

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

    // 新增：根据得分计算coin
    private int calculateCoinsFromScore(int score) {
        // 基础奖励：每100分获得1个coin
        int baseCoins = score / 100;

        // 额外奖励：根据波数给予bonus
        int waveBonus = 0;
        if (mWaveManager != null) {
            waveBonus = mWaveManager.getWaveNumber() * 5;
        }

        // 确保至少获得一些coin
        return Math.max(10, baseCoins + waveBonus);
    }

    private int getCurrentWaveNumber() {
        return mWaveManager != null ? mWaveManager.getWaveNumber() : 0;
    }
}
