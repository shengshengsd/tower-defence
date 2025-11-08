package ch.logixisland.anuto.business.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class CoinManager {
    private static final String TAG = "CoinManager";
    private static final String PREF_NAME = "coin_preferences";
    private static final String KEY_COINS = "coins";

    // 设置硬币上限为 999999
    private static final int MAX_COINS = 999999;

    private final SharedPreferences mPreferences;
    private int mCoins;

    public CoinManager(Context context) {
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mCoins = mPreferences.getInt(KEY_COINS, 0);
        Log.d(TAG, "CoinManager initialized with coins: " + mCoins);
    }

    public void addCoins(int amount) {
        if (amount > 0) {
            int oldCoins = mCoins;
            mCoins += amount;

            // 检查是否超过上限
            if (mCoins > MAX_COINS) {
                mCoins = MAX_COINS;
                Log.d(TAG, "Coin limit reached! Coins capped at " + MAX_COINS);
            }

            saveCoins();
            Log.d(TAG, "Added " + amount + " coins. Old: " + oldCoins + ", New: " + mCoins);
        }
    }

    public boolean spendCoins(int amount) {
        if (amount <= 0) {
            Log.w(TAG, "Invalid spend amount: " + amount);
            return false;
        }

        if (mCoins < amount) {
            Log.w(TAG, "Insufficient coins. Required: " + amount + ", Available: " + mCoins);
            return false;
        }

        int oldCoins = mCoins;
        mCoins -= amount;
        saveCoins();
        Log.d(TAG, "Spent " + amount + " coins. Old: " + oldCoins + ", New: " + mCoins);
        return true;
    }

    public int getCoins() {
        return mCoins;
    }

    public void setCoins(int coins) {
        if (coins < 0) {
            Log.w(TAG, "Attempt to set negative coins: " + coins);
            return;
        }

        int oldCoins = mCoins;
        mCoins = Math.min(coins, MAX_COINS); // 确保不超过上限

        if (coins > MAX_COINS) {
            Log.d(TAG, "Coin limit applied. Requested: " + coins + ", Set to: " + mCoins);
        }

        saveCoins();
        Log.d(TAG, "Coins set from " + oldCoins + " to " + mCoins);
    }

    // 新增：获取硬币上限
    public int getMaxCoins() {
        return MAX_COINS;
    }

    // 新增：检查是否达到上限
    public boolean isAtMax() {
        return mCoins >= MAX_COINS;
    }

    // 新增：获取剩余可添加的硬币数量
    public int getRemainingCapacity() {
        return Math.max(0, MAX_COINS - mCoins);
    }

    private void saveCoins() {
        mPreferences.edit().putInt(KEY_COINS, mCoins).apply();
    }

    public void reset() {
        int oldCoins = mCoins;
        mCoins = 0;
        saveCoins();
        Log.d(TAG, "Coins reset from " + oldCoins + " to 0");
    }
}
