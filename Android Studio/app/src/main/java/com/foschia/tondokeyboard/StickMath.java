package com.foschia.tondokeyboard;

/** Pure geometry used by the analog sticks. All angles are in degrees, math orientation (counter-clockwise, y up). */
public final class StickMath
{
	private StickMath() {}

	public static float closestMultiple(float value, int divider)
	{
		int sign = value < 0 ? -1 : 1;
		float abs = Math.abs(value);
		float rest = abs % divider;
		if (rest > divider * 0.5f) rest = rest - divider;
		return sign * (abs - rest);
	}

	public static float squircleAngle(float angle, float curvature, float closestMultiple, float threshold)
	{
		float sign = angle < closestMultiple ? 1 : -1;
		return (float) (sign * threshold * Math.pow(1 - Math.abs(Math.pow((closestMultiple - angle) / threshold, curvature)), 1 / curvature)
				+ (closestMultiple - (sign * threshold)));
	}

	/**
	 * Angle of the finger movement in [0, 360). Screen coordinates are y down, so dy is flipped here.
	 *
	 * @param reverse the "reverse" setting: the stick moves opposite to the finger (needle on the other side)
	 */
	public static float angle(float x0, float y0, float x1, float y1, boolean reverse)
	{
		float dx = x1 - x0;
		float dy = -(y1 - y0);
		if (reverse)
		{
			dx = -dx;
			dy = -dy;
		}
		float a = (float) Math.toDegrees(Math.atan2(dy, dx));
		if (a < 0) a += 360;
		return a;
	}

	/** Applies the "sticky" / "very sticky" settings. */
	public static float stick(float angle, int sectors, boolean sticky, boolean verySticky)
	{
		int amplitude = 360 / sectors;
		if (sticky)
		{
			float closest = closestMultiple(angle, amplitude);
			float threshold = amplitude * 0.5f;
			if (Math.abs(closest - angle) < threshold)
				return squircleAngle(angle, 3, closest, threshold);
		}
		else if (verySticky)
		{
			return closestMultiple(angle, amplitude);
		}
		return angle;
	}

	/** Picks the sector for an angle. Sector i spans [-amp/2 + amp*i, -amp/2 + amp*(i+1)). */
	public static Dir sector(float angle, Dir[] valid)
	{
		int amplitude = 360 / valid.length;
		Dir result = valid[0];
		float first = -(amplitude / 2f);
		for (int i = 0; i < valid.length; i++)
		{
			if (angle >= first + amplitude * i && angle < first + amplitude * (i + 1)) result = valid[i];
		}
		return result;
	}

	/** Direction selected by a drag of given distance and (already stickified) angle. */
	public static Dir select(float angle, float distance, float stickWidth, int externalDistanceFactor, Dir[] valid)
	{
		if (distance > stickWidth * (externalDistanceFactor * 0.01f)) return sector(angle, valid);
		return Dir.CENTER;
	}
}
