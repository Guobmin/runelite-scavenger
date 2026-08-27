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
		int bestSamePlaneDist = Integer.MAX_VALUE;
		SpawnLocation bestAnyPlane = null;
		int bestAnyPlaneDist = Integer.MAX_VALUE;

		for (SpawnLocation loc : locations)
		{
			int dx = loc.x - playerLocation.getX();
			int dy = loc.y - playerLocation.getY();
			int distSq = dx * dx + dy * dy;

			if (distSq < bestAnyPlaneDist)
			{
				bestAnyPlaneDist = distSq;
				bestAnyPlane = loc;
			}

			if (loc.plane == playerLocation.getPlane() && distSq < bestSamePlaneDist)
			{
				bestSamePlaneDist = distSq;
				bestSamePlane = loc;
			}
		}

		if (bestSamePlane != null)
		{
			return new Result(bestSamePlane, true, navTarget(bestSamePlane, playerLocation));
		}

		return new Result(bestAnyPlane, false, navTarget(bestAnyPlane, playerLocation));
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
