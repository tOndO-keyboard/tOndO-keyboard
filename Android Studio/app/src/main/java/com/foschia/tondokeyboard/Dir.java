package com.foschia.tondokeyboard;

/** Directions of a tondo, same ordinal values as the original CardinalDirection enum. */
public enum Dir
{
	CENTER, EAST, NORTH_EAST, NORTH, NORTH_WEST, WEST, SOUTH_WEST, SOUTH, SOUTH_EAST;

	/** Sectors of a tondo (6 clovees of 60 degrees), counter-clockwise starting at -30 degrees (math orientation, y up). */
	public static final Dir[] TONDO_SECTORS = {EAST, NORTH_EAST, NORTH_WEST, WEST, SOUTH_WEST, SOUTH_EAST};

	/** Sectors of the cursor stick (8 clovees of 45 degrees). */
	public static final Dir[] CURSOR_SECTORS = {EAST, NORTH_EAST, NORTH, NORTH_WEST, WEST, SOUTH_WEST, SOUTH, SOUTH_EAST};
}
