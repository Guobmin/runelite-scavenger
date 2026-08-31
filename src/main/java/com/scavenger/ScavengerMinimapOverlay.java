package com.scavenger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class ScavengerMinimapOverlay extends Overlay
{
	private static final int DOT_SIZE = 8;
	private static final int ARROW_LENGTH = 20;
	private static final int ARROW_WIDTH = 16;
	private static final int EDGE_MARGIN = 10;
	private static final int EDGE_DISTANCE_LOCAL = 10 * Perspective.LOCAL_TILE_SIZE;
	// Matches Quest Helper's DetailedQuestStep flash rate: a tick counter
	// wrapping every 4 GameTicks, visible for the first half - 2 ticks
	// (1.2s) on, 2 ticks (1.2s) off.
	private static final int FLASH_PERIOD_TICKS = 4;

	private final Client client;
	private final TargetManager targetManager;
	private final ScavengerConfig config;
	private int currentTick;

	@Inject
	ScavengerMinimapOverlay(Client client, TargetManager targetManager, ScavengerConfig config)
	{
		this.client = client;
		this.targetManager = targetManager;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(Overlay.PRIORITY_LOW);
	}

	@Override
	public java.awt.Dimension render(Graphics2D graphics)
	{
		NearestLocationFinder.Result result = targetManager.getActiveResult();
		if (result == null || client.getLocalPlayer() == null)
		{
			return null;
		}

		WorldPoint worldPoint = result.navTarget;
		LocalPoint targetLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		Point minimapPoint = targetLocal == null ? null : Perspective.localToMinimap(client, targetLocal);

		if (minimapPoint != null)
		{
			drawDot(graphics, minimapPoint, config.highlightColor());
			return null;
		}

		Point edgePoint = edgeMinimapPoint(worldPoint);
		if (edgePoint != null && currentTick < FLASH_PERIOD_TICKS / 2)
		{
			drawArrow(graphics, edgePoint, config.highlightColor());
		}

		return null;
	}

	void onGameTick()
	{
		currentTick = (currentTick + 1) % FLASH_PERIOD_TICKS;
	}

	/**
	 * Finds where the target direction crosses the rim of the actual minimap widget, so the
	 * arrow always sits at the visible edge of the minimap regardless of zoom level.
	 */
	private Point edgeMinimapPoint(WorldPoint worldPoint)
	{
		Widget minimapWidget = getMinimapDrawWidget();
		if (minimapWidget == null || minimapWidget.isHidden())
		{
			return null;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		LocalPoint playerLocal = client.getLocalPlayer().getLocalLocation();
		if (playerLocal == null)
		{
			return null;
		}

		int[] offset = directionOffset(
			worldPoint.getX() - playerLocation.getX(),
			worldPoint.getY() - playerLocation.getY(),
			EDGE_DISTANCE_LOCAL);
		if (offset[0] == 0 && offset[1] == 0)
		{
			return null;
		}

		LocalPoint probeLocal = new LocalPoint(
			playerLocal.getX() + offset[0],
			playerLocal.getY() + offset[1],
			client.getTopLevelWorldView());
		Point probe = Perspective.localToMinimap(client, probeLocal);
		if (probe == null)
		{
			return null;
		}

		Point widgetLoc = minimapWidget.getCanvasLocation();
		double centerX = widgetLoc.getX() + minimapWidget.getWidth() / 2.0;
		double centerY = widgetLoc.getY() + minimapWidget.getHeight() / 2.0;
		double radius = Math.min(minimapWidget.getWidth(), minimapWidget.getHeight()) / 2.0 - EDGE_MARGIN;

		double dx = probe.getX() - centerX;
		double dy = probe.getY() - centerY;
		double magnitude = Math.hypot(dx, dy);
		if (magnitude == 0)
		{
			return null;
		}

		return new Point(
			(int) Math.round(centerX + dx / magnitude * radius),
			(int) Math.round(centerY + dy / magnitude * radius));
	}

	private Widget getMinimapDrawWidget()
	{
		if (client.isResized())
		{
			if (client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1)
			{
				return client.getWidget(InterfaceID.ToplevelPreEoc.MINIMAP);
			}
			return client.getWidget(InterfaceID.ToplevelOsrsStretch.MINIMAP);
		}
		return client.getWidget(InterfaceID.Toplevel.MINIMAP);
	}

	static int[] directionOffset(int dx, int dy, int distance)
	{
		double magnitude = Math.hypot(dx, dy);
		if (magnitude == 0)
		{
			return new int[] {0, 0};
		}
		return new int[] {
			(int) Math.round(dx / magnitude * distance),
			(int) Math.round(dy / magnitude * distance)
		};
	}

	private static void drawDot(Graphics2D graphics, Point center, Color color)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			int half = DOT_SIZE / 2;
			g.setColor(color);
			g.fillOval(center.getX() - half, center.getY() - half, DOT_SIZE, DOT_SIZE);
			g.setColor(Color.BLACK);
			g.drawOval(center.getX() - half, center.getY() - half, DOT_SIZE, DOT_SIZE);
		}
		finally
		{
			g.dispose();
		}
	}

	private void drawArrow(Graphics2D graphics, Point center, Color color)
	{
		double angle = arrowAngle(center);

		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			AffineTransform transform = new AffineTransform();
			transform.translate(center.getX(), center.getY());
			transform.rotate(angle + Math.PI / 2);
			g.setTransform(combine(g.getTransform(), transform));

			g.setColor(Color.BLACK);
			g.fillPolygon(arrowHead(ARROW_LENGTH, ARROW_WIDTH));
			g.setColor(color);
			g.fillPolygon(arrowHead(ARROW_LENGTH - 3, ARROW_WIDTH - 3));
		}
		finally
		{
			g.dispose();
		}
	}

	private double arrowAngle(Point target)
	{
		Player player = client.getLocalPlayer();
		Point playerPoint = player == null ? null : player.getMinimapLocation();
		if (playerPoint == null)
		{
			return 0;
		}
		return Math.atan2(target.getY() - playerPoint.getY(), target.getX() - playerPoint.getX());
	}

	private static Polygon arrowHead(int length, int width)
	{
		Polygon polygon = new Polygon();
		polygon.addPoint(0, -length);
		polygon.addPoint(-width / 2, length / 2);
		polygon.addPoint(width / 2, length / 2);
		return polygon;
	}

	private static AffineTransform combine(AffineTransform base, AffineTransform local)
	{
		AffineTransform combined = new AffineTransform(base);
		combined.concatenate(local);
		return combined;
	}
}
