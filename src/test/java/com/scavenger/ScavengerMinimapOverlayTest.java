package com.scavenger;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class ScavengerMinimapOverlayTest
{
	@Test
	public void scalesToExactDistanceAlongDirection()
	{
		// 3-4-5 triangle scaled by 10: distance 50 along (3,4) direction -> (30,40)
		int[] offset = ScavengerMinimapOverlay.directionOffset(3, 4, 50);

		assertArrayEquals(new int[] {30, 40}, offset);
	}

	@Test
	public void handlesAxisAlignedDirection()
	{
		int[] offset = ScavengerMinimapOverlay.directionOffset(10, 0, 100);

		assertArrayEquals(new int[] {100, 0}, offset);
	}

	@Test
	public void returnsZeroOffsetWhenTargetIsPlayerTile()
	{
		int[] offset = ScavengerMinimapOverlay.directionOffset(0, 0, 100);

		assertArrayEquals(new int[] {0, 0}, offset);
	}
}
