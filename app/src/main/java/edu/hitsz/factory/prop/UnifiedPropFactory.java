package edu.hitsz.factory.prop;

import edu.hitsz.prop.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 统一道具工厂：可按固定类型创建，或启用随机掉落按概率/随机类型创建
 */
public class UnifiedPropFactory implements PropFactory {

    private final Random random = new Random();
    private final PropType[] types = PropType.values();
    private PropType type = PropType.BLOOD;
    private int speedY = 5;
    // 随机掉落配置
    private boolean randomEnabled = false;
    private double dropRate = 0.0;

    public UnifiedPropFactory() {
    }

    public UnifiedPropFactory(PropType type) {
        this.type = type;
    }

    public UnifiedPropFactory setType(PropType type) {
        this.type = type;
        return this;
    }

    public UnifiedPropFactory setSpeedY(int speedY) {
        this.speedY = speedY;
        return this;
    }

    public UnifiedPropFactory enableRandomDrop(double dropRate) {
        this.randomEnabled = true;
        this.dropRate = dropRate;
        return this;
    }

    public UnifiedPropFactory disableRandomDrop() {
        this.randomEnabled = false;
        return this;
    }

    public UnifiedPropFactory setDropRate(double dropRate) {
        this.dropRate = dropRate;
        return this;
    }

    @Override
    public AbstractProp create(int x, int y) {
        switch (type) {
            case BOMB:
                return new BombProp(x, y, 0, speedY);
            case BULLET:
                return new BulletProp(x, y, 0, speedY);
            case BULLET_PLUS:
                return new BulletPlusProp(x, y, 0, speedY);
            case BLOOD:
            default:
                return new BloodProp(x, y, 0, speedY);
        }
    }

    /**
     * 根据 dropRate 生成0或1个随机类型道具（仅在启用随机掉落时使用）
     */
    public List<AbstractProp> generate(int x, int y) {
        List<AbstractProp> res = new ArrayList<>();
        if (!randomEnabled) {
            return res;
        }
        if (random.nextDouble() < dropRate) {
            PropType t = types[random.nextInt(types.length)];
            this.setType(t);
            res.add(this.create(x, y));
        }
        return res;
    }
}