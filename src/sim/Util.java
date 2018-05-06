package sim;

import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;

class Util {

    static float nextFloat(float minValue, float maxValue) {
        return MathUtils.randomFloat(minValue, maxValue);
    }

    static Vec2 polarToRectangular(float magnitude, float angle){
        float x = magnitude * MathUtils.cos(angle);
        float y = magnitude * MathUtils.sin(angle);
        return new Vec2(x, y);
    }

    static float[] rectangularToPolar(Vec2 point){
        float[] polar = new float[2]; //0 = magnitude, 1 = angle
        polar[0] = (float) Math.hypot(point.x, point.y);
        polar[1] = MathUtils.atan2(point.y, point.x);
        return polar;
    }

    static float toPixelX(float x) {
        return x * 50f;
    }

    static float toPixelY(float y) {
        return 600 - y * 50f;
    }

    static float round2(float number) {
        int pow = 10;
        for (int i = 1; i < 2; i++)
            pow *= 10;
        float tmp = number * pow;
        return (float) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
    }

}
