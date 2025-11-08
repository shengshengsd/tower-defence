package ch.logixisland.anuto.business.game;

public class ShopItem {
    public enum ItemType {
        TOWER_UNLOCK,
        STARTING_LIVES_BOOST,
        STARTING_CREDITS_BOOST,
        PERMANENT_UPGRADE
    }

    private final String mId;
    private final String mName;
    private final String mDescription;
    private final int mPrice;
    private final String mIconName; // 改为使用图标名称而不是资源ID
    private final ItemType mType;
    private final Object mEffectData;
    private boolean mPurchased;

    public ShopItem(String id, String name, String description, int price, String iconName,
                    ItemType type, Object effectData) {
        mId = id;
        mName = name;
        mDescription = description;
        mPrice = price;
        mIconName = iconName;
        mType = type;
        mEffectData = effectData;
        mPurchased = false;
    }

    // Getters
    public String getId() { return mId; }
    public String getName() { return mName; }
    public String getDescription() { return mDescription; }
    public int getPrice() { return mPrice; }
    public String getIconName() { return mIconName; } // 改为返回图标名称
    public ItemType getType() { return mType; }
    public Object getEffectData() { return mEffectData; }
    public boolean isPurchased() { return mPurchased; }

    public void setPurchased(boolean purchased) {
        mPurchased = purchased;
    }
}
