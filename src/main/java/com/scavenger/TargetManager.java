package com.scavenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Singleton
class TargetManager
{
	private final ItemDatabase itemDatabase;
	private final Client client;
	private final ScavengerConfig config;
	private volatile ItemSpawn activeItem;
	private volatile NearestLocationFinder.Result activeResult;

	@Inject
	TargetManager(ItemDatabase itemDatabase, Client client, ScavengerConfig config)
	{
		this.itemDatabase = itemDatabase;
		this.client = client;
		this.config = config;
	}

	List<ItemSpawn> search(String query)
	{
		String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		boolean includeMembers = config.accountType() == AccountType.MEMBERS;
		List<ItemSpawn> results = new ArrayList<>();
		for (ItemSpawn item : itemDatabase.getAll())
		{
			if (!needle.isEmpty() && !item.name.toLowerCase(Locale.ROOT).contains(needle))
			{
				continue;
			}
			if (!item.availableLocations(includeMembers).isEmpty())
			{
				results.add(item);
			}
		}
		return results;
	}

	void setActiveItem(ItemSpawn item)
	{
		this.activeItem = item;
		this.activeResult = null;
	}

	void clearActiveItem()
	{
		this.activeItem = null;
		this.activeResult = null;
	}

	ItemSpawn getActiveItem()
	{
		return activeItem;
	}

	void refresh(WorldPoint playerLocation)
	{
		if (activeItem == null)
		{
			return;
		}

		boolean includeMembers = config.accountType() == AccountType.MEMBERS;
		List<SpawnLocation> candidates = activeItem.availableLocations(includeMembers);
		if (!activeItem.isObject())
		{
			candidates = filterKnownAbsent(candidates, loc -> isGroundItemAbsent(loc, activeItem.itemId));
		}

		activeResult = NearestLocationFinder.findNearest(playerLocation, candidates);
	}

	NearestLocationFinder.Result getActiveResult()
	{
		return activeResult;
	}

	/**
	 * Drops locations the predicate reports as absent, unless that would empty the list -
	 * a location we can't currently see is better than tracking nothing.
	 */
	static List<SpawnLocation> filterKnownAbsent(List<SpawnLocation> locations, Predicate<SpawnLocation> isAbsent)
	{
		List<SpawnLocation> present = new ArrayList<>();
		for (SpawnLocation loc : locations)
		{
			if (!isAbsent.test(loc))
			{
				present.add(loc);
			}
		}
		return present.isEmpty() ? locations : present;
	}

	private boolean isGroundItemAbsent(SpawnLocation loc, int itemId)
	{
		if (client == null)
		{
			return false;
		}

		WorldPoint worldPoint = new WorldPoint(loc.x, loc.y, loc.plane);
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (localPoint == null)
		{
			return false;
		}

		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		if (loc.plane < 0 || loc.plane >= tiles.length
			|| sceneX < 0 || sceneX >= tiles[loc.plane].length
			|| sceneY < 0 || sceneY >= tiles[loc.plane][sceneX].length)
		{
			return false;
		}

		Tile tile = tiles[loc.plane][sceneX][sceneY];
		if (tile == null)
		{
			return false;
		}

		List<TileItem> groundItems = tile.getGroundItems();
		if (groundItems == null)
		{
			return true;
		}

		for (TileItem tileItem : groundItems)
		{
			if (tileItem.getId() == itemId)
			{
				return false;
			}
		}

		return true;
	}
}
