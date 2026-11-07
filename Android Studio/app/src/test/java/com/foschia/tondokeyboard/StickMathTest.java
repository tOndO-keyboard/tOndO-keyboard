package com.foschia.tondokeyboard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StickMathTest
{
	private static final float EPS = 0.01f;

	@Test
	public void dragRightIsAngleZero()
	{
		assertEquals(0f, StickMath.angle(100, 100, 200, 100, false), EPS);
	}

	@Test
	public void dragUpOnScreenIsNinetyDegrees()
	{
		// screen y grows downwards: moving up means a smaller y
		assertEquals(90f, StickMath.angle(100, 100, 100, 50, false), EPS);
	}

	@Test
	public void dragDownOnScreenIs270()
	{
		assertEquals(270f, StickMath.angle(100, 100, 100, 150, false), EPS);
	}

	@Test
	public void reverseFlipsAngle()
	{
		assertEquals(180f, StickMath.angle(100, 100, 200, 100, true), EPS);
	}

	@Test
	public void sectorsFollowTondoLayout()
	{
		Dir[] v = Dir.TONDO_SECTORS;
		assertEquals(Dir.EAST, StickMath.sector(0, v));
		assertEquals(Dir.EAST, StickMath.sector(29, v));
		assertEquals(Dir.NORTH_EAST, StickMath.sector(60, v));
		assertEquals(Dir.NORTH_WEST, StickMath.sector(120, v));
		assertEquals(Dir.WEST, StickMath.sector(180, v));
		assertEquals(Dir.SOUTH_WEST, StickMath.sector(240, v));
		assertEquals(Dir.SOUTH_EAST, StickMath.sector(300, v));
		assertEquals(Dir.EAST, StickMath.sector(330, v));
	}

	@Test
	public void swipeUpRightOnScreenSelectsNorthEast()
	{
		float a = StickMath.angle(0, 0, 50, -86, false);
		assertEquals(Dir.NORTH_EAST, StickMath.sector(a, Dir.TONDO_SECTORS));
	}

	@Test
	public void shortDragIsCentre()
	{
		assertEquals(Dir.CENTER, StickMath.select(0, 10, 280, 7, Dir.TONDO_SECTORS));
		assertEquals(Dir.EAST, StickMath.select(0, 25, 280, 7, Dir.TONDO_SECTORS));
	}

	@Test
	public void verySticky()
	{
		assertEquals(60f, StickMath.stick(50f, 6, false, true), EPS);
		assertEquals(0f, StickMath.stick(20f, 6, false, true), EPS);
		assertEquals(50f, StickMath.stick(50f, 6, false, false), EPS);
	}

	@Test
	public void stickySnapsTowardsClosestMultiple()
	{
		float out = StickMath.stick(10f, 6, true, false);
		assertEquals(true, Math.abs(out) < 10f);
	}

	@Test
	public void cursorSectorsHaveEight()
	{
		assertEquals(Dir.NORTH, StickMath.sector(90, Dir.CURSOR_SECTORS));
		assertEquals(Dir.SOUTH_WEST, StickMath.sector(225, Dir.CURSOR_SECTORS));
	}
}
