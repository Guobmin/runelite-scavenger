package com.scavenger;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class ScavengerWorldOverlay extends Overlay
{
	// Cave entrance tiles (e.g. Edgeville's "trapdoor by a coffin") can stack an
	// unrelated decorative object on the same tile as the actual trapdoor - the
	// generic trapdoor ids are reused across dozens of dungeon entrances, so
	// prefer one of these over whatever the scene's object array returns first.
	private static final Set<Integer> TRAPDOOR_OBJECT_IDS = Set.of(
		ObjectID.TRAPDOOR_NONACTIVE, ObjectID.TRAPDOOR, ObjectID.TRAPDOOR_LEVEL1,
		ObjectID.TRAPDOOR_OPEN, ObjectID.TRAPDOOR_OPEN_LEVEL1);

	private final Client client;
	private final TargetManager targetManager;
	private final ScavengerConfig config;

	@Inject
	ScavengerWorldOverlay(Client client, TargetManager targetManager, ScavengerConfig config)
	{
		this.client = client;
		this.targetManager = targetManager;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(Overlay.PRIORITY_LOW);
	}

	@Override
	public java.awt.Dimension render(Graphics2D graphics)
	{
		NearestLocationFinder.Result result = targetManager.getActiveResult();
		Player player = client.getLocalPlayer();
		if (result == null || player == null)
		{
			return null;
		}

		WorldPoint worldPoint = result.navTarget;
		if (worldPoint.getPlane() != player.getWorldLocation().getPlane())
		{
			return null;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (localPoint == null)
		{
			return null;
		}

		Tile tile = client.getTopLevelWorldView().getScene().getTiles()[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];

		// Aiming at the cave entrance rather than the real spawn tile - highlight
		// whatever door/trapdoor/cave-mouth object sits there, same as Quest
		// Helper does for its step objects. There's no ground item to look for at
		// an entrance tile.
		boolean atEntrance = !worldPoint.equals(new WorldPoint(result.location.x, result.location.y, result.location.plane));
		ItemSpawn item = targetManager.getActiveItem();
		Shape highlight;
		if (atEntrance)
		{
			highlight = findEntranceObjectShape(tile);
		}
		else if (item != null && item.isObject())
		{
			highlight = findObjectShape(tile);
		}
		else
		{
			highlight = findGroundItemShape(client, tile, localPoint, item == null ? -1 : item.itemId);
		}

		if (highlight == null)
		{
			highlight = Perspective.getCanvasTilePoly(client, localPoint);
		}

		if (highlight != null)
		{
			OverlayUtil.renderPolygon(graphics, highlight, config.highlightColor());
		}

		return null;
	}

	private static Shape findEntranceObjectShape(Tile tile)
	{
		if (tile == null)
		{
			return null;
		}

		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null && TRAPDOOR_OBJECT_IDS.contains(gameObject.getId()))
			{
				Shape clickbox = gameObject.getClickbox();
				if (clickbox != null)
				{
					return clickbox;
				}
			}
		}

		// Trapdoors are frequently a GroundObject (or WallObject) rather than a
		// plain GameObject - the coffin at Edgeville is the GameObject on this
		// tile, so without this check the loop above finds nothing and falls
		// straight through to the coffin via findObjectShape's first-match order.
		if (tile.getWallObject() != null && TRAPDOOR_OBJECT_IDS.contains(tile.getWallObject().getId()))
		{
			Shape clickbox = tile.getWallObject().getClickbox();
			if (clickbox != null)
			{
				return clickbox;
			}
		}

		if (tile.getGroundObject() != null && TRAPDOOR_OBJECT_IDS.contains(tile.getGroundObject().getId()))
		{
			Shape clickbox = tile.getGroundObject().getClickbox();
			if (clickbox != null)
			{
				return clickbox;
			}
		}

		return findObjectShape(tile);
	}

	private static Shape findObjectShape(Tile tile)
	{
		if (tile == null)
		{
			return null;
		}

		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null)
			{
				Shape clickbox = gameObject.getClickbox();
				if (clickbox != null)
				{
					return clickbox;
				}
			}
		}

		if (tile.getWallObject() != null)
		{
			Shape clickbox = tile.getWallObject().getClickbox();
			if (clickbox != null)
			{
				return clickbox;
			}
		}

		if (tile.getDecorativeObject() != null)
		{
			Shape clickbox = tile.getDecorativeObject().getClickbox();
			if (clickbox != null)
			{
				return clickbox;
			}
		}

		if (tile.getGroundObject() != null)
		{
			return tile.getGroundObject().getClickbox();
		}

		return null;
	}

	private static Shape findGroundItemShape(Client client, Tile tile, LocalPoint localPoint, int itemId)
	{
		if (tile == null)
		{
			return null;
		}

		List<TileItem> groundItems = tile.getGroundItems();
		if (groundItems == null)
		{
			return null;
		}

		for (TileItem tileItem : groundItems)
		{
			if (tileItem.getId() == itemId)
			{
				int height = tile.getItemLayer() != null ? tile.getItemLayer().getHeight() : 0;
				return Perspective.getCanvasTilePoly(client, localPoint, height);
			}
		}

		return null;
	}
}
