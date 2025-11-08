package ch.logixisland.anuto.view.game;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ch.logixisland.anuto.R;
import ch.logixisland.anuto.AnutoApplication;
import ch.logixisland.anuto.business.game.ShopItem;
import ch.logixisland.anuto.business.game.ShopManager;
import ch.logixisland.anuto.engine.theme.ActivityType;
import ch.logixisland.anuto.view.AnutoActivity;

import java.util.List;

public class ShopActivity extends AnutoActivity {
    private static final String TAG = "ShopActivity";

    private ShopManager mShopManager;
    private TextView mCoinsText;

    @Override
    protected ActivityType getActivityType() {
        return ActivityType.Popup;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        Log.d(TAG, "ShopActivity created");

        try {
            mShopManager = ((AnutoApplication) getApplication()).getGameFactory().getShopManager();
            Log.d(TAG, "ShopManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ShopManager: " + e.getMessage(), e);
            Toast.makeText(this, "Shop system not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        updateCoinsDisplay();
        loadShopItems();
    }

    private void initializeViews() {
        mCoinsText = findViewById(R.id.txt_coins);
        if (mCoinsText == null) {
            Log.e(TAG, "Coins text view not found");
        }

        Button btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                finish();
            });
        } else {
            Log.e(TAG, "Back button not found");
        }
    }

    private void updateCoinsDisplay() {
        if (mShopManager != null && mCoinsText != null) {
            int coins = mShopManager.getCoinManager().getCoins();
            mCoinsText.setText(getString(R.string.coins_format, coins));
            Log.d(TAG, "Coins display updated: " + coins);
        }
    }

    private void loadShopItems() {
        if (mShopManager == null) {
            Log.e(TAG, "ShopManager is null, cannot load items");
            return;
        }

        List<ShopItem> availableItems = mShopManager.getShopRepository().getAvailableItems();
        Log.d(TAG, "Found " + availableItems.size() + " available items");

        for (int i = 0; i < Math.min(availableItems.size(), 5); i++) {
            ShopItem item = availableItems.get(i);
            setupShopItemView(i, item);
        }
    }

    private void setupShopItemView(int index, ShopItem item) {
        int itemLayoutId = getResources().getIdentifier("shop_item_" + (index + 1), "id", getPackageName());
        View itemView = findViewById(itemLayoutId);

        if (itemView != null) {
            itemView.setVisibility(View.VISIBLE);

            ImageView icon = itemView.findViewById(R.id.item_icon);
            TextView name = itemView.findViewById(R.id.item_name);
            TextView description = itemView.findViewById(R.id.item_description);
            TextView price = itemView.findViewById(R.id.item_price);
            Button buyButton = itemView.findViewById(R.id.btn_buy);

            if (icon != null && name != null && description != null && price != null && buyButton != null) {
                // 设置商品信息
                int iconResId = getIconResourceId(item.getIconName());
                if (iconResId != 0) {
                    icon.setImageResource(iconResId);
                } else {
                    Log.w(TAG, "Icon resource not found: " + item.getIconName());
                    // 设置默认图标
                    icon.setImageResource(R.mipmap.icon);
                }

                name.setText(item.getName());
                description.setText(item.getDescription());
                price.setText(getString(R.string.price_format, item.getPrice()));

                // 设置购买按钮
                buyButton.setOnClickListener(v -> {
                    Log.d(TAG, "Buy button clicked for item: " + item.getName());
                    if (mShopManager.purchaseItem(item.getId())) {
                        Toast.makeText(this, R.string.purchase_successful, Toast.LENGTH_SHORT).show();
                        updateCoinsDisplay();
                        itemView.setVisibility(View.GONE);
                        Log.d(TAG, "Item purchased successfully: " + item.getName());
                    } else {
                        Toast.makeText(this, R.string.insufficient_coins, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Purchase failed: insufficient coins for " + item.getName());
                    }
                });

                Log.d(TAG, "Shop item view setup completed for: " + item.getName());
            } else {
                Log.e(TAG, "Some views not found in shop item layout");
            }
        } else {
            Log.e(TAG, "Shop item layout not found for index: " + index);
        }
    }

    private int getIconResourceId(String iconName) {
        int resId = getResources().getIdentifier(iconName, "drawable", getPackageName());
        if (resId == 0) {
            Log.w(TAG, "Icon resource not found: " + iconName);
        }
        return resId;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ShopActivity resumed");
        // 更新金币显示
        updateCoinsDisplay();
    }
}
