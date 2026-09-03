package com.scavenger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

@Slf4j
@Singleton
class TargetManager
{
	// Troubleshooting log for tracking-mismatch reports - see README's "Help and issues" section.
	// Capped in size so it never grows unbounded on disk.
	private static final Path DEBUG_LOG_FILE = RuneLite.RUNELITE_DIR.toPath().resolve("scavenger").resolve("debug.log");
	private static final long MAX_DEBUG_LOG_BYTES = 200 * 1024;

	private final ItemDatabase itemDatabase;
	private final Client client;
	private final ScavengerConfig config;
	private final ScheduledExecutorService executor;
	private volatile ItemSpawn activeItem;
	private volatile NearestLocationFinder.Result activeResult;

	@Inject
	TargetManager(ItemDatabase itemDatabase, Client client, ScavengerConfig config, ScheduledExecutorService executor)
	{
		this.itemDatabase = itemDatabase;
		this.client = client;
		this.config = config;
		this.executor = executor;
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
			logAbsence(loc, itemId, "no ground items");
			return true;
		}

		for (TileItem tileItem : groundItems)
		{
			if (tileItem.getId() == itemId)
			{
				return false;
			}
		}

		String foundIds = groundItems.stream().map(i -> String.valueOf(i.getId())).collect(Collectors.joining(","));
		logAbsence(loc, itemId, "found ids [" + foundIds + "]");
		return true;
	}

	private void logAbsence(SpawnLocation loc, int itemId, String detail)
	{
		String line = String.format("%s wanted id %d absent at %d,%d,%d: %s%n",
			Instant.now(), itemId, loc.x, loc.y, loc.plane, detail);
		executor.execute(() -> appendDebugLine(line));
	}

	// Runs off the client thread - disk IO must never block onGameTick.
	private static void appendDebugLine(String line)
	{
		try
		{
			Files.createDirectories(DEBUG_LOG_FILE.getParent());
			byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
			boolean overSize = Files.exists(DEBUG_LOG_FILE) && Files.size(DEBUG_LOG_FILE) > MAX_DEBUG_LOG_BYTES;
			StandardOpenOption sizeOption = overSize ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.APPEND;
			Files.write(DEBUG_LOG_FILE, bytes, StandardOpenOption.CREATE, sizeOption);
		}
		catch (IOException e)
		{
			log.debug("scavenger: failed to write debug log", e);
		}
	}
}
