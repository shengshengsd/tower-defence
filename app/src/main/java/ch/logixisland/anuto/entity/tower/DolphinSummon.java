package ch.logixisland.anuto.entity.tower;

import android.graphics.Canvas;

import ch.logixisland.anuto.R;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.entity.Entity;
import ch.logixisland.anuto.engine.logic.loop.TickTimer;
import ch.logixisland.anuto.engine.render.Layers;
import ch.logixisland.anuto.engine.render.sprite.SpriteInstance;
import ch.logixisland.anuto.engine.render.sprite.SpriteTemplate;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformation;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformer;
import ch.logixisland.anuto.engine.render.sprite.StaticSprite;
import ch.logixisland.anuto.entity.EntityTypes;
import ch.logixisland.anuto.entity.enemy.Enemy;
import ch.logixisland.anuto.entity.shot.WaterShot;
import ch.logixisland.anuto.util.RandomUtils;
import ch.logixisland.anuto.util.iterator.StreamIterator;
import ch.logixisland.anuto.util.math.Vector2;

public class DolphinSummon extends Entity implements SpriteTransformation {

    private final static float MOVE_SPEED = 1.2f;
    private final static float ATTACK_RANGE = 1.5f;
    private final static float ATTACK_COOLDOWN = 1.5f;
    private final static float LIFETIME = 12.0f;
    private final static float PATROL_RANGE = 2.0f;

    private static class StaticData {
        SpriteTemplate mSpriteTemplate;
    }

    private final MakotoDolphin mOwner;
    private final float mDamage;
    private boolean mActive = true;
    private float mAngle = 0f;
    private Enemy mCurrentTarget;
    private Vector2 mHomePosition;
    private Vector2 mPatrolTarget;

    private final StaticSprite mSprite;
    private final TickTimer mLifeTimer;
    private final TickTimer mAttackTimer;
    private final TickTimer mPatrolTimer;

    public DolphinSummon(MakotoDolphin owner, Vector2 position, float damage) {
        super(owner.getGameEngine());

        mOwner = owner;
        mDamage = damage;
        mHomePosition = position;
        setPosition(position);

        // 初始化精灵
        StaticData s = (StaticData) getStaticData();
        mSprite = getSpriteFactory().createStatic(Layers.TOWER, s.mSpriteTemplate);
        mSprite.setListener(this);
        mSprite.setIndex(RandomUtils.next(4));

        mLifeTimer = TickTimer.createInterval(LIFETIME);
        mAttackTimer = TickTimer.createInterval(ATTACK_COOLDOWN);
        mPatrolTimer = TickTimer.createInterval(2.0f); // 巡逻目标更新间隔

        // 重置攻击计时器，使其可以立即攻击
        mAttackTimer.tick();
        setNewPatrolTarget();
    }

    @Override
    public String getEntityName() {
        return "dolphinSummon";
    }

    @Override
    public int getEntityType() {
        return EntityTypes.EFFECT;
    }

    @Override
    public Object initStatic() {
        StaticData s = new StaticData();

        s.mSpriteTemplate = getSpriteFactory().createTemplate(R.attr.dolphinSummon, 4);
        s.mSpriteTemplate.setMatrix(0.5f, 0.5f, null, -90f);

        return s;
    }

    @Override
    public void init() {
        super.init();
        getGameEngine().add(mSprite);
    }

    @Override
    public void clean() {
        super.clean();
        getGameEngine().remove(mSprite);
    }

    @Override
    public void tick() {
        super.tick();

        // 检查生命周期
        if (mLifeTimer.tick()) {
            mActive = false;
            remove();
            return;
        }

        // 寻找目标
        if (mCurrentTarget == null ||
                getDistanceTo(mCurrentTarget) > ATTACK_RANGE * 2f) {
            findNewTarget();
        }

        // 移动逻辑
        if (mCurrentTarget != null) {
            // 向目标移动
            moveToTarget(mCurrentTarget.getPosition());

            // 攻击逻辑
            if (mAttackTimer.tick() && getDistanceTo(mCurrentTarget) <= ATTACK_RANGE) {
                attackTarget();
            }
        } else {
            // 巡逻逻辑
            if (mPatrolTimer.tick() || getDistanceTo(mPatrolTarget) < 0.2f) {
                setNewPatrolTarget();
            }
            moveToTarget(mPatrolTarget);
        }
    }

    private void findNewTarget() {
        StreamIterator<Enemy> enemies = getGameEngine().getEntitiesByType(EntityTypes.ENEMY)
                .filter(Entity.inRange(getPosition(), ATTACK_RANGE * 2f))
                .cast(Enemy.class);

        if (enemies.hasNext()) {
            mCurrentTarget = enemies.min(Entity.distanceTo(getPosition()));
        } else {
            mCurrentTarget = null;
        }
    }

    private void moveToTarget(Vector2 target) {
        // 修复：使用 getDirectionTo 替代有问题的 norm() 方法
        Vector2 direction = getDirectionTo(target);

        mAngle = direction.angle();

        float distanceToMove = MOVE_SPEED / GameEngine.TARGET_FRAME_RATE;
        Vector2 newPosition = getPosition().add(direction.mul(distanceToMove));

        // 确保不会离主场位置太远
        if (newPosition.distanceTo(mHomePosition) <= PATROL_RANGE) {
            setPosition(newPosition);
        } else {
            // 如果太远，向主场位置移动
            // 修复：同样使用 getDirectionTo 替代有问题的 norm() 方法
            direction = getDirectionTo(mHomePosition);
            mAngle = direction.angle();
            setPosition(getPosition().add(direction.mul(distanceToMove)));
        }
    }

    private void setNewPatrolTarget() {
        float angle = RandomUtils.next(360f);
        float distance = RandomUtils.next(0.5f, PATROL_RANGE);
        mPatrolTarget = Vector2.polar(distance, angle).add(mHomePosition);
    }

    private void attackTarget() {
        if (mCurrentTarget != null ) {
            // 创建水弹攻击
            WaterShot shot = new WaterShot(this, getPosition(), mCurrentTarget, mDamage);
            getGameEngine().add(shot);

            // 报告伤害给所有者
            mOwner.reportDamageInflicted(mDamage);
        }
    }

    @Override
    public void draw(SpriteInstance sprite, Canvas canvas) {
        SpriteTransformer.translate(canvas, getPosition());
        canvas.rotate(mAngle);
    }

    public boolean isActive() {
        return mActive;
    }

    public MakotoDolphin getOwner() {
        return mOwner;
    }
}
