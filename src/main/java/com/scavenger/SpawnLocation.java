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
	}
}
