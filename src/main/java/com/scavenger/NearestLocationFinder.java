package com.scavenger;

import java.util.List;
import net.runelite.api.coords.WorldPoint;

class NearestLocationFinder
{
	static class Result
	{
		final SpawnLocation location;
		final boolean samePlane;

		Result(SpawnLocation location, boolean samePlane)
		{
			this.location = location;
			this.samePlane = samePlane;
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
			return new Result(bestSamePlane, true);
		}

		return new Result(bestAnyPlane, false);
	}
}
