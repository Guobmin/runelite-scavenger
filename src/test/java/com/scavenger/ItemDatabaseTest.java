package com.scavenger;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ItemDatabaseTest
{
	private final Gson gson = new Gson();

	@Test
	public void parsesWellFormedJson()
	{
		String json = "[{\"name\":\"Bucket\",\"itemId\":1925,\"type\":\"GROUND_ITEM\",\"locations\":[{\"x\":1,\"y\":2,\"plane\":0,\"areaLabel\":\"Test\"}]}]";
		InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

		List<ItemSpawn> items = ItemDatabase.parse(gson, stream);

		assertEquals(1, items.size());
		assertEquals("Bucket", items.get(0).name);
		assertEquals(1925, items.get(0).itemId);
		assertFalse(items.get(0).isObject());
		assertEquals(1, items.get(0).locations.get(0).x);
	}

	@Test
	public void parsesObjectTypeSpawn()
	{
		String json = "[{\"name\":\"Cabbage\",\"itemId\":1965,\"type\":\"OBJECT\",\"locations\":[{\"x\":1,\"y\":2,\"plane\":0,\"areaLabel\":\"Test\"}]}]";
		InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

		List<ItemSpawn> items = ItemDatabase.parse(gson, stream);

		assertTrue(items.get(0).isObject());
	}

	@Test
	public void returnsEmptyListForNullStream()
	{
		List<ItemSpawn> items = ItemDatabase.parse(gson, null);

		assertTrue(items.isEmpty());
	}

	@Test
	public void returnsEmptyListForMalformedJson()
	{
		InputStream stream = new ByteArrayInputStream("not json".getBytes(StandardCharsets.UTF_8));

		List<ItemSpawn> items = ItemDatabase.parse(gson, stream);

		assertTrue(items.isEmpty());
	}

	@Test
	public void bundledItemsJsonLoadsAndIsNonEmpty()
	{
		ItemDatabase db = new ItemDatabase(gson);

		assertTrue(db.getAll().size() > 0);
	}

	@Test
	public void parsesLocationRequirementWhenPresent()
	{
		String json = "[{\"name\":\"Blue dragon scale\",\"itemId\":243,\"type\":\"GROUND_ITEM\","
			+ "\"locations\":[{\"x\":1,\"y\":2,\"plane\":0,\"areaLabel\":\"Myths' Guild basement\","
			+ "\"members\":true,\"requirement\":\"Requires completion of Dragon Slayer II\"}]}]";
		InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

		List<ItemSpawn> items = ItemDatabase.parse(gson, stream);

		assertEquals("Requires completion of Dragon Slayer II", items.get(0).locations.get(0).requirement);
	}

	@Test
	public void requirementIsNullWhenAbsentFromJson()
	{
		String json = "[{\"name\":\"Bucket\",\"itemId\":1925,\"type\":\"GROUND_ITEM\",\"locations\":[{\"x\":1,\"y\":2,\"plane\":0,\"areaLabel\":\"Test\"}]}]";
		InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

		List<ItemSpawn> items = ItemDatabase.parse(gson, stream);

		assertNull(items.get(0).locations.get(0).requirement);
	}
}
