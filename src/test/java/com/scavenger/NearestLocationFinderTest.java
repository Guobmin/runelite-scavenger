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

	private static SpawnLocation.Entrance entrance(int x, int y, int plane)
	{
		SpawnLocation.Entrance e = new SpawnLocation.Entrance();
		e.x = x;
		e.y = y;
		e.plane = plane;
		return e;
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

	@Test
	public void navTargetIsRealSpawnWhenNoEntrance()
	{
		SpawnLocation loc = spawn(3210, 3210, 0);
		WorldPoint player = new WorldPoint(3200, 3200, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(player, Collections.singletonList(loc));

		assertEquals(new WorldPoint(3210, 3210, 0), result.navTarget);
	}

	@Test
	public void navTargetIsEntranceWhenPlayerOutsideSpawnRegion()
	{
		// Daeyalt essence mine coordinates - far underground region, well outside
		// any surface region a player could be standing in.
		SpawnLocation cave = spawn(3670, 9772, 0);
		cave.entrance = entrance(3417, 3479, 0);
		WorldPoint player = new WorldPoint(3200, 3200, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(player, Collections.singletonList(cave));

		assertEquals(new WorldPoint(3417, 3479, 0), result.navTarget);
	}

	@Test
	public void navTargetIsRealSpawnWhenPlayerAlreadyInsideSpawnRegion()
	{
		SpawnLocation cave = spawn(3670, 9772, 0);
		cave.entrance = entrance(3417, 3479, 0);
		WorldPoint playerInsideCave = new WorldPoint(3665, 9770, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(playerInsideCave, Collections.singletonList(cave));

		assertEquals(new WorldPoint(3670, 9772, 0), result.navTarget);
	}

	@Test
	public void selectionComparesAgainstEntranceNotRawUndergroundCoordinate()
	{
		// Regression: the Dwarven Mine's real spawn sits far underground (y+6400
		// or so), so comparing raw coordinates made a cave spawn a short walk
		// from its entrance always lose to a merely-farther surface spawn. The
		// player here stands right next to the mine entrance - the mine spawn
		// must win even though its raw coordinate looks thousands of tiles away.
		SpawnLocation mine = spawn(2985, 9817, 0);
		mine.entrance = entrance(3018, 3450, 0);
		SpawnLocation farSurfaceAlternative = spawn(1694, 3270, 0);
		List<SpawnLocation> locations = Arrays.asList(farSurfaceAlternative, mine);
		WorldPoint player = new WorldPoint(3015, 3448, 0);

		NearestLocationFinder.Result result = NearestLocationFinder.findNearest(player, locations);

		assertEquals(mine, result.location);
		assertEquals(new WorldPoint(3018, 3450, 0), result.navTarget);
	}
}
