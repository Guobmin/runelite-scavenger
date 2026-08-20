package com.scavenger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ItemDatabase
{
	private final List<ItemSpawn> items;

	@Inject
	ItemDatabase(Gson gson)
	{
		this(parse(gson, ItemDatabase.class.getResourceAsStream("items.json")));
	}

	private ItemDatabase(List<ItemSpawn> items)
	{
		this.items = items;
	}

	static ItemDatabase fromItems(List<ItemSpawn> items)
	{
		return new ItemDatabase(items);
	}

	static List<ItemSpawn> parse(Gson gson, InputStream stream)
	{
		if (stream == null)
		{
			log.warn("items.json resource not found");
			return Collections.emptyList();
		}

		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			ItemSpawn[] parsed = gson.fromJson(reader, ItemSpawn[].class);
			return parsed == null ? Collections.emptyList() : List.of(parsed);
		}
		catch (JsonSyntaxException | IOException e)
		{
			log.warn("Failed to parse items.json", e);
			return Collections.emptyList();
		}
	}

	List<ItemSpawn> getAll()
	{
		return items;
	}
}
