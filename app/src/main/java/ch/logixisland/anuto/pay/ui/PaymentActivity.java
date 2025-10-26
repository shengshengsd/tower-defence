package ch.logixisland.anuto.pay.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import ch.logixisland.anuto.R;
import ch.logixisland.anuto.view.game.GameActivity;

public class PaymentActivity extends Activity {

    private static final String PREFS_NAME = "payment_prefs";
    private static final String KEY_PAID = "is_payment_completed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Button btnPay = findViewById(R.id.btn_pay);
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 模拟支付成功，保存支付状态
                setPaymentCompleted();

                // 显示支付成功提示
                Toast.makeText(PaymentActivity.this, "支付成功！正在进入游戏...", Toast.LENGTH_SHORT).show();

                // 进入游戏主界面
                Intent intent = new Intent(PaymentActivity.this, GameActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setPaymentCompleted() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_PAID, true);
        editor.apply();
    }

    // 提供一个静态方法用于检查支付状态
    public static boolean isPaymentCompleted(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_PAID, false);
    }
}
