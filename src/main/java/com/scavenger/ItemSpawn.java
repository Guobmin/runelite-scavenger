package com.scavenger;

import java.util.ArrayList;
import java.util.List;

class ItemSpawn
{
	String name;
	int itemId;
	SpawnType type;
	List<SpawnLocation> locations;

	boolean isObject()
	{
		return type == SpawnType.OBJECT;
	}

	List<SpawnLocation> availableLocations(boolean includeMembers)
	{
		if (includeMembers)
		{
			return locations;
		}

		List<SpawnLocation> free = new ArrayList<>();
		for (SpawnLocation loc : locations)
		{
			if (!loc.members)
			{
				free.add(loc);
			}
		}
		return free;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
