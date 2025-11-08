package ch.logixisland.anuto.business.game;

import ch.logixisland.anuto.engine.logic.GameEngine;

public class ShopManager {
    private final CoinManager mCoinManager;
    private final ShopRepository mShopRepository;
    private final GameEngine mGameEngine;

    public ShopManager(CoinManager coinManager, ShopRepository shopRepository, GameEngine gameEngine) {
        mCoinManager = coinManager;
        mShopRepository = shopRepository;
        mGameEngine = gameEngine;
    }

    public boolean purchaseItem(String itemId) {
        ShopItem item = mShopRepository.getItemById(itemId);
        if (item == null || item.isPurchased()) {
            return false;
        }

        if (mCoinManager.spendCoins(item.getPrice())) {
            mShopRepository.markItemPurchased(itemId);
            applyItemEffect(item);
            return true;
        }

        return false;
    }

    private void applyItemEffect(ShopItem item) {
        switch (item.getType()) {
            case TOWER_UNLOCK:
                // Tower unlocks are handled elsewhere
                break;
            case STARTING_LIVES_BOOST:
            case STARTING_CREDITS_BOOST:
            case PERMANENT_UPGRADE:
                // These effects are applied when starting a new game
                break;
        }
    }

    public CoinManager getCoinManager() {
        return mCoinManager;
    }

    public ShopRepository getShopRepository() {
        return mShopRepository;
    }
}
