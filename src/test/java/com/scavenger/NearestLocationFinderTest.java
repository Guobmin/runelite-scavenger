package com.scavenger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NearestLocationFinderTest
{
	private static SpawnLocation spawn(int x, int y, int plane)
	{
		SpawnLocation s = new SpawnLocation();
		s.x = x;
		s.y = y;
		s.plane = plane;
		s.areaLabel = "test";
		return s;
	}

	@Test
	public void picksClosestOnSamePlane()
	{
		SpawnLocation near = spawn(3210, 3210, 0);
		SpawnLocation far = spawn(3300, 3300, 0);
		List<SpawnLocation> locations = Arrays.asList(far, near);
		WorldPoint player = new WorldPoint(3200, 3200, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(player, locations);

		assertEquals(near, result.location);
		assertTrue(result.samePlane);
	}

	@Test
	public void fallsBackToDifferentPlaneWhenNoneMatch()
	{
		SpawnLocation onlyCandidate = spawn(3210, 3210, 1);
		List<SpawnLocation> locations = Collections.singletonList(onlyCandidate);
		WorldPoint player = new WorldPoint(3200, 3200, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(player, locations);

		assertEquals(onlyCandidate, result.location);
		assertTrue(!result.samePlane);
	}

	@Test
	public void fallbackPicksGloballyNearestAcrossPlanes()
	{
		SpawnLocation nearestPlane1 = spawn(3210, 3210, 1);
		SpawnLocation fartherPlane2 = spawn(3300, 3300, 2);
		SpawnLocation farthestPlane3 = spawn(3400, 3400, 3);
		List<SpawnLocation> locations = Arrays.asList(farthestPlane3, fartherPlane2, nearestPlane1);
		WorldPoint player = new WorldPoint(3200, 3200, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(player, locations);

		assertEquals(nearestPlane1, result.location);
		assertTrue(!result.samePlane);
	}

	@Test
	public void usesEuclideanNotManhattanDistance()
	{
		// From (3200,3200): manhattanCloser is at offset (10,0) -> Euclidean 10.0, Manhattan 10.
		// euclideanCloser is at offset (7,7) -> Euclidean ~9.899, Manhattan 14.
		// Euclidean-nearest must pick euclideanCloser even though it's Manhattan-farther.
		SpawnLocation manhattanCloser = spawn(3210, 3200, 0);
		SpawnLocation euclideanCloser = spawn(3207, 3207, 0);
		List<SpawnLocation> locations = Arrays.asList(manhattanCloser, euclideanCloser);
		WorldPoint player = new WorldPoint(3200, 3200, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(player, locations);

		assertEquals(euclideanCloser, result.location);
		assertTrue(result.samePlane);
	}

	@Test
	public void returnsNullForEmptyLocations()
	{
		WorldPoint player = new WorldPoint(3200, 3200, 0);

		assertNull(NearestLocationFinder.findNearest(player, Collections.emptyList()));
		assertNull(NearestLocationFinder.findNearest(player, null));
	}
}
