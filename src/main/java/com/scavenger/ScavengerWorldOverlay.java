package com.scavenger;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class ScavengerWorldOverlay extends Overlay
{
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
		if (result == null || !result.samePlane)
		{
			return null;
		}

		ItemSpawn item = targetManager.getActiveItem();
		SpawnLocation loc = result.location;
		WorldPoint worldPoint = new WorldPoint(loc.x, loc.y, loc.plane);
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (localPoint == null)
		{
			return null;
		}

		Tile tile = client.getTopLevelWorldView().getScene().getTiles()[loc.plane][localPoint.getSceneX()][localPoint.getSceneY()];
		Shape highlight = item != null && item.isObject()
			? findObjectShape(tile)
			: findGroundItemShape(client, tile, localPoint, item == null ? -1 : item.itemId);

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
