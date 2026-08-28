package com.scavenger;

import java.util.List;
import net.runelite.api.coords.WorldPoint;

class NearestLocationFinder
{
	static class Result
	{
		final SpawnLocation location;
		final boolean samePlane;
		final WorldPoint navTarget;

		Result(SpawnLocation location, boolean samePlane, WorldPoint navTarget)
		{
			this.location = location;
			this.samePlane = samePlane;
			this.navTarget = navTarget;
		}
	}

	static Result findNearest(WorldPoint playerLocation, List<SpawnLocation> locations)
	{
		if (locations == null || locations.isEmpty())
		{
			return null;
		}

		SpawnLocation bestSamePlane = null;
		WorldPoint bestSamePlaneTarget = null;
		int bestSamePlaneDist = Integer.MAX_VALUE;
		SpawnLocation bestAnyPlane = null;
		WorldPoint bestAnyPlaneTarget = null;
		int bestAnyPlaneDist = Integer.MAX_VALUE;

		for (SpawnLocation loc : locations)
		{
			// Compare against the entrance (when applicable), not the raw spawn
			// coordinate: OSRS shifts underground instances thousands of tiles
			// away in y, so a cave spawn a short walk from its entrance would
			// otherwise always lose to a genuinely-farther surface alternative.
			WorldPoint target = navTarget(loc, playerLocation);
			int dx = target.getX() - playerLocation.getX();
			int dy = target.getY() - playerLocation.getY();
			int distSq = dx * dx + dy * dy;

			if (distSq < bestAnyPlaneDist)
			{
				bestAnyPlaneDist = distSq;
				bestAnyPlane = loc;
				bestAnyPlaneTarget = target;
			}

			if (loc.plane == playerLocation.getPlane() && distSq < bestSamePlaneDist)
			{
				bestSamePlaneDist = distSq;
				bestSamePlane = loc;
				bestSamePlaneTarget = target;
			}
		}

		if (bestSamePlane != null)
		{
			return new Result(bestSamePlane, true, bestSamePlaneTarget);
		}

		return new Result(bestAnyPlane, false, bestAnyPlaneTarget);
	}

	/**
	 * Where the locator should aim. If the spawn has a curated cave entrance and the
	 * player isn't yet in that spawn's map region, aim at the entrance instead of the
	 * real tile - the real tile is often unreachable in a straight line from outside.
	 */
	private static WorldPoint navTarget(SpawnLocation loc, WorldPoint playerLocation)
	{
		WorldPoint real = new WorldPoint(loc.x, loc.y, loc.plane);
		if (loc.entrance == null || playerLocation.getRegionID() == real.getRegionID())
		{
			return real;
		}

		return new WorldPoint(loc.entrance.x, loc.entrance.y, loc.entrance.plane);
	}
}
