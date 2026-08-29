package com.scavenger;

class SpawnLocation
{
	int x;
	int y;
	int plane;
	String areaLabel;
	boolean members;
	String requirement;
	// Nullable — only present for cave/dungeon spawns, curated via the wiki
	// scraper. Lets the locator aim at the entrance instead of straight
	// through rock at a spawn tile you can't walk to from the surface.
	Entrance entrance;

	static class Entrance
	{
		int x;
		int y;
		int plane;
		// Nullable — set only when the exact entrance object has been verified
		// in-game (e.g. via RuneLite Dev Tools). When absent, the overlay falls
		// back to a generic trapdoor-name search near the curated tile.
		Integer objectId;
	}
}
