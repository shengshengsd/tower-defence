package ch.logixisland.anuto.entity.tower;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

import ch.logixisland.anuto.R;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.entity.Entity;
import ch.logixisland.anuto.engine.logic.entity.EntityFactory;
import ch.logixisland.anuto.engine.logic.loop.TickTimer;
import ch.logixisland.anuto.engine.render.Layers;
import ch.logixisland.anuto.engine.render.sprite.SpriteInstance;
import ch.logixisland.anuto.engine.render.sprite.SpriteTemplate;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformation;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformer;
import ch.logixisland.anuto.engine.render.sprite.StaticSprite;
import ch.logixisland.anuto.engine.sound.Sound;
import ch.logixisland.anuto.entity.enemy.WeaponType;
import ch.logixisland.anuto.entity.shot.WaterShot;
import ch.logixisland.anuto.util.RandomUtils;
import ch.logixisland.anuto.util.math.Vector2;

public class MakotoDolphin extends Tower implements SpriteTransformation {

    public final static String ENTITY_NAME = "makotoDolphin";
    private final static float DOLPHIN_SPAWN_INTERVAL = 4.0f;
    private final static int MAX_DOLPHIN_COUNT = 2;
    private final static float DOLPHIN_DAMAGE_MULTIPLIER = 0.6f;
    private final static float ENHANCE_MAX_DOLPHIN_COUNT = 1;
    private final static float ENHANCE_DOLPHIN_DAMAGE = 0.1f;

    private final static TowerProperties TOWER_PROPERTIES = new TowerProperties.Builder()
            .setValue(800)
            .setDamage(150) // 本体伤害
            .setRange(3.0f)
            .setReload(2.0f)
            .setMaxLevel(8)
            .setWeaponType(WeaponType.Bullet)
            .setEnhanceBase(1.3f)
            .setEnhanceCost(120)
            .setEnhanceDamage(25)
            .setEnhanceRange(0.1f)
            .setEnhanceReload(0.1f)
            .setUpgradeLevel(3)
            .build();

    public static class Factory extends EntityFactory {
        @Override
        public Entity create(GameEngine gameEngine) {
            return new MakotoDolphin(gameEngine);
        }
    }

    public static class Persister extends TowerPersister {

    }

    private static class StaticData {
        SpriteTemplate mSpriteTemplateBase;
        SpriteTemplate mSpriteTemplateTower;
    }

    private float mAngle = 90f;
    private int mMaxDolphinCount = MAX_DOLPHIN_COUNT;
    private float mDolphinDamageMultiplier = DOLPHIN_DAMAGE_MULTIPLIER;
    private final Aimer mAimer = new Aimer(this);
    private final TickTimer mDolphinSpawnTimer = TickTimer.createInterval(DOLPHIN_SPAWN_INTERVAL);

    private final StaticSprite mSpriteBase;
    private final StaticSprite mSpriteTower;
    private final Sound mSound;

    private final List<DolphinSummon> mActiveDolphins = new ArrayList<>();

    public MakotoDolphin(GameEngine gameEngine) {
        super(gameEngine, TOWER_PROPERTIES);
        StaticData s = (StaticData) getStaticData();

        mSpriteBase = getSpriteFactory().createStatic(Layers.TOWER_BASE, s.mSpriteTemplateBase);
        mSpriteBase.setListener(this);
        mSpriteBase.setIndex(RandomUtils.next(4));

        mSpriteTower = getSpriteFactory().createStatic(Layers.TOWER, s.mSpriteTemplateTower);
        mSpriteTower.setListener(this);
        mSpriteTower.setIndex(RandomUtils.next(4));

        mSound = getSoundFactory().createSound(R.raw.explosive1_chk);
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    public Object initStatic() {
        StaticData s = new StaticData();

        s.mSpriteTemplateBase = getSpriteFactory().createTemplate(R.attr.base4, 4);
        s.mSpriteTemplateBase.setMatrix(1f, 1f, null, null);

        s.mSpriteTemplateTower = getSpriteFactory().createTemplate(R.attr.makotoDolphinTower, 4);
        s.mSpriteTemplateTower.setMatrix(0.8f, 0.8f, new Vector2(0.4f, 0.4f), -90f);

        return s;
    }

    @Override
    public void init() {
        super.init();

        getGameEngine().add(mSpriteBase);
        getGameEngine().add(mSpriteTower);
    }

    @Override
    public void clean() {
        super.clean();

        getGameEngine().remove(mSpriteBase);
        getGameEngine().remove(mSpriteTower);

        // 清理所有活跃的海豚
        for (DolphinSummon dolphin : mActiveDolphins) {
            dolphin.remove();
        }
        mActiveDolphins.clear();
    }

    @Override
    public void enhance() {
        super.enhance();
        mMaxDolphinCount += ENHANCE_MAX_DOLPHIN_COUNT;
        mDolphinDamageMultiplier += ENHANCE_DOLPHIN_DAMAGE;
    }

    @Override
    public void tick() {
        super.tick();
        mAimer.tick();

        // 清理不活跃的海豚
        for (int i = mActiveDolphins.size() - 1; i >= 0; i--) {
            DolphinSummon dolphin = mActiveDolphins.get(i);
            if (!dolphin.isActive()) {
                mActiveDolphins.remove(i);
            }
        }

        // 尝试召唤海豚
        if (mActiveDolphins.size() < mMaxDolphinCount && mDolphinSpawnTimer.tick()) {
            spawnDolphin();
        }

        // 本体攻击逻辑
        if (mAimer.getTarget() != null && isReloaded()) {
            mAngle = getAngleTo(mAimer.getTarget());

            // 发射水弹攻击
            WaterShot shot = new WaterShot(this, getPosition(), mAimer.getTarget(), getDamage());
            getGameEngine().add(shot);
            mSound.play();

            setReloaded(false);
        }
    }

    private void spawnDolphin() {
        if (mActiveDolphins.size() < mMaxDolphinCount) {
            float dolphinDamage = getDamage() * mDolphinDamageMultiplier;
            DolphinSummon dolphin = new DolphinSummon(this, getPosition(), dolphinDamage);

            dolphin.addListener(new Entity.Listener() {
                @Override
                public void entityRemoved(Entity entity) {
                    mActiveDolphins.remove(entity);
                }
            });

            mActiveDolphins.add(dolphin);
            getGameEngine().add(dolphin);
        }
    }

    @Override
    public Aimer getAimer() {
        return mAimer;
    }

    @Override
    public void draw(SpriteInstance sprite, Canvas canvas) {
        SpriteTransformer.translate(canvas, getPosition());

        if (sprite == mSpriteTower) {
            canvas.rotate(mAngle);
        }
    }

    @Override
    public void preview(Canvas canvas) {
        mSpriteBase.draw(canvas);
        mSpriteTower.draw(canvas);
    }

    @Override
    public List<TowerInfoValue> getTowerInfoValues() {
        List<TowerInfoValue> properties = new ArrayList<>();
        properties.add(new TowerInfoValue(R.string.damage, getDamage()));
        properties.add(new TowerInfoValue(R.string.dolphin_damage, getDamage() * mDolphinDamageMultiplier));
        properties.add(new TowerInfoValue(R.string.dolphin_count, mActiveDolphins.size()));
        properties.add(new TowerInfoValue(R.string.reload, getReloadTime()));
        properties.add(new TowerInfoValue(R.string.range, getRange()));
        properties.add(new TowerInfoValue(R.string.inflicted, getDamageInflicted()));
        return properties;
    }

    public int getActiveDolphinCount() {
        return mActiveDolphins.size();
    }
}
