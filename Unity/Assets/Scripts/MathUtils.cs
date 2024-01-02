using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public static class MathUtils
{
    public static float GetClosestMultiple(float value, int divider) {
        int sign = value < 0 ? -1 : +1;
        float absValue = Mathf.Abs(value);
        float rest = absValue % divider;
        if (rest > divider * 0.5f)
            rest = rest - divider;
        float result = sign * (absValue - rest);
        return result;
    }

    public static bool ApproximatelyEquals(float a, float b, float threshold) {
        return Mathf.Abs(a - b) <= threshold;
    }

    public static float SquircleAngle(float angle, float curvature, float closestMultiple, float threshold) {
        float sign = angle < closestMultiple ? 1 : -1;
        float result = sign * threshold * Mathf.Pow((1 - Mathf.Abs(Mathf.Pow((closestMultiple - angle) / threshold, curvature))), (1 / curvature)) + (closestMultiple - (sign * threshold));
        return result;
    }

    public static float ElasticLerp(float from, float to, float t)
    {
        const float c4 = (2 * Mathf.PI) / 3;
        float newT = t == 0 ? 0 : t == 1 ? 1 : 
                Mathf.Pow(2, -10 * t) * Mathf.Sin((t * 10 - 0.75f) * c4) + 1;
        return Mathf.Lerp(from, to, newT);
    }
}
