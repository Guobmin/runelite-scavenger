package com.scavenger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TargetManagerTest
{
	private static ItemSpawn item(String name, SpawnLocation... locations)
	{
		ItemSpawn item = new ItemSpawn();
		item.name = name;
		item.locations = Arrays.asList(locations);
		return item;
	}

	private static SpawnLocation spawn(int x, int y, int plane)
	{
		return spawn(x, y, plane, false);
	}

	private static SpawnLocation spawn(int x, int y, int plane, boolean members)
	{
		SpawnLocation s = new SpawnLocation();
		s.x = x;
		s.y = y;
		s.plane = plane;
		s.areaLabel = "test";
		s.members = members;
		return s;
	}

	private static ScavengerConfig freeConfig()
	{
		return new ScavengerConfig()
		{
			@Override
			public AccountType accountType()
			{
				return AccountType.FREE;
			}
		};
	}

	@Test
	public void searchIsCaseInsensitiveSubstringMatch()
	{
		ItemSpawn bucket = item("Bucket", spawn(0, 0, 0));
		ItemSpawn knife = item("Knife", spawn(0, 0, 0));
		TargetManager manager = new TargetManager(ItemDatabase.fromItems(Arrays.asList(bucket, knife)), null, new ScavengerConfig() {});

		List<ItemSpawn> results = manager.search("buck");

		assertEquals(Collections.singletonList(bucket), results);
	}

	@Test
	public void blankQueryReturnsAllAvailableItems()
	{
		ItemSpawn bucket = item("Bucket", spawn(0, 0, 0));
		TargetManager manager = new TargetManager(ItemDatabase.fromItems(Collections.singletonList(bucket)), null, new ScavengerConfig() {});

		assertEquals(Collections.singletonList(bucket), manager.search(""));
		assertEquals(Collections.singletonList(bucket), manager.search("   "));
	}

	@Test
	public void refreshComputesNearestResultForActiveItem()
	{
		SpawnLocation near = spawn(3210, 3210, 0);
		SpawnLocation far = spawn(3300, 3300, 0);
		ItemSpawn bucket = item("Bucket", far, near);
		TargetManager manager = new TargetManager(ItemDatabase.fromItems(Collections.singletonList(bucket)), null, new ScavengerConfig() {});

		manager.setActiveItem(bucket);
		manager.refresh(new WorldPoint(3200, 3200, 0));

		assertEquals(near, manager.getActiveResult().location);
	}

	@Test
	public void clearActiveItemResetsResult()
	{
		ItemSpawn bucket = item("Bucket", spawn(3210, 3210, 0));
		TargetManager manager = new TargetManager(ItemDatabase.fromItems(Collections.singletonList(bucket)), null, new ScavengerConfig() {});

		manager.setActiveItem(bucket);
		manager.refresh(new WorldPoint(3200, 3200, 0));
		manager.clearActiveItem();

		assertNull(manager.getActiveItem());
		assertNull(manager.getActiveResult());
	}

	@Test
	public void availableLocationsIncludesMembersLocationsWhenIncludeMembersTrue()
	{
		SpawnLocation free = spawn(0, 0, 0, false);
		SpawnLocation members = spawn(1, 1, 0, true);
		ItemSpawn item = item("Bucket", free, members);

		assertEquals(Arrays.asList(free, members), item.availableLocations(true));
	}

	@Test
	public void availableLocationsExcludesMembersLocationsWhenIncludeMembersFalse()
	{
		SpawnLocation free = spawn(0, 0, 0, false);
		SpawnLocation members = spawn(1, 1, 0, true);
		ItemSpawn item = item("Bucket", free, members);

		assertEquals(Collections.singletonList(free), item.availableLocations(false));
	}

	@Test
	public void searchHidesMembersOnlyItemsUnderFreeAccountType()
	{
		ItemSpawn bucket = item("Bucket", spawn(0, 0, 0, false));
		ItemSpawn membersItem = item("Bucket of sand", spawn(1, 1, 0, true));
		TargetManager manager = new TargetManager(ItemDatabase.fromItems(Arrays.asList(bucket, membersItem)), null, freeConfig());

		List<ItemSpawn> results = manager.search("bucket");

		assertEquals(Collections.singletonList(bucket), results);
	}

	@Test
	public void refreshOnlyConsidersFreeLocationsUnderFreeAccountType()
	{
		SpawnLocation membersNear = spawn(3210, 3210, 0, true);
		SpawnLocation freeFar = spawn(3300, 3300, 0, false);
		ItemSpawn bucket = item("Bucket", membersNear, freeFar);
		TargetManager manager = new TargetManager(ItemDatabase.fromItems(Collections.singletonList(bucket)), null, freeConfig());

		manager.setActiveItem(bucket);
		manager.refresh(new WorldPoint(3200, 3200, 0));

		assertEquals(freeFar, manager.getActiveResult().location);
	}

	@Test
	public void filterKnownAbsentRemovesLocationsReportedAbsent()
	{
		SpawnLocation a = spawn(1, 1, 0);
		SpawnLocation b = spawn(2, 2, 0);

		List<SpawnLocation> filtered = TargetManager.filterKnownAbsent(Arrays.asList(a, b), loc -> loc == a);

		assertEquals(Collections.singletonList(b), filtered);
	}

	@Test
	public void filterKnownAbsentFallsBackToFullListWhenEverythingIsAbsent()
	{
		List<SpawnLocation> locations = Arrays.asList(spawn(1, 1, 0), spawn(2, 2, 0));

		List<SpawnLocation> filtered = TargetManager.filterKnownAbsent(locations, loc -> true);

		assertEquals(locations, filtered);
	}
}
