package ch.logixisland.anuto.business.game;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ShopRepository {
    private static final String PREF_NAME = "shop_preferences";
    private static final String KEY_PURCHASED_ITEMS = "purchased_items";

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final List<ShopItem> mShopItems;

    public ShopRepository(Context context) {
        mContext = context;
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mShopItems = new ArrayList<>();
        initializeShopItems();
        loadPurchasedItems();
    }

    private void initializeShopItems() {
        // 使用图标名称而不是资源ID
        mShopItems.add(new ShopItem(
                "unlock_makoto_dolphin",
                "Makoto Dolphin",
                "Unlock the powerful Makoto Dolphin tower",
                500,
                "ic_tower_makoto_dolphin",
                ShopItem.ItemType.TOWER_UNLOCK,
                "MakotoDolphin"
        ));

        mShopItems.add(new ShopItem(
                "unlock_rocket_launcher",
                "Rocket Launcher",
                "Unlock the explosive Rocket Launcher tower",
                300,
                "ic_tower_rocket",
                ShopItem.ItemType.TOWER_UNLOCK,
                "RocketLauncher"
        ));

        mShopItems.add(new ShopItem(
                "extra_starting_lives",
                "Extra Lives",
                "Start with +2 extra lives in every game",
                200,
                "ic_extra_lives",
                ShopItem.ItemType.STARTING_LIVES_BOOST,
                2
        ));

        mShopItems.add(new ShopItem(
                "starting_credits_boost",
                "Credit Boost",
                "Start with +100 credits in every game",
                150,
                "ic_credit_boost",
                ShopItem.ItemType.STARTING_CREDITS_BOOST,
                100
        ));

        mShopItems.add(new ShopItem(
                "tower_damage_boost",
                "Damage Boost",
                "Permanently increase all tower damage by 5%",
                400,
                "ic_damage_boost",
                ShopItem.ItemType.PERMANENT_UPGRADE,
                1.05f
        ));
    }

    public List<ShopItem> getAvailableItems() {
        List<ShopItem> available = new ArrayList<>();
        for (ShopItem item : mShopItems) {
            if (!item.isPurchased()) {
                available.add(item);
            }
        }
        return available;
    }

    public List<ShopItem> getPurchasedItems() {
        List<ShopItem> purchased = new ArrayList<>();
        for (ShopItem item : mShopItems) {
            if (item.isPurchased()) {
                purchased.add(item);
            }
        }
        return purchased;
    }

    public ShopItem getItemById(String id) {
        for (ShopItem item : mShopItems) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public void markItemPurchased(String itemId) {
        ShopItem item = getItemById(itemId);
        if (item != null) {
            item.setPurchased(true);
            savePurchasedItems();
        }
    }

    private void loadPurchasedItems() {
        String purchasedJson = mPreferences.getString(KEY_PURCHASED_ITEMS, "[]");
        try {
            JSONArray purchasedArray = new JSONArray(purchasedJson);
            for (int i = 0; i < purchasedArray.length(); i++) {
                String itemId = purchasedArray.getString(i);
                ShopItem item = getItemById(itemId);
                if (item != null) {
                    item.setPurchased(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePurchasedItems() {
        try {
            JSONArray purchasedArray = new JSONArray();
            for (ShopItem item : mShopItems) {
                if (item.isPurchased()) {
                    purchasedArray.put(item.getId());
                }
            }
            mPreferences.edit().putString(KEY_PURCHASED_ITEMS, purchasedArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetPurchases() {
        for (ShopItem item : mShopItems) {
            item.setPurchased(false);
        }
        mPreferences.edit().remove(KEY_PURCHASED_ITEMS).apply();
    }
}
