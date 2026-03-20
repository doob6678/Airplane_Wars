package edu.hitsz.factory.prop;

import edu.hitsz.prop.AbstractProp;

/**
 * 道具工厂接口
 */
public interface PropFactory {
    AbstractProp create(int x, int y);
}
