package com.scavenger;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Scavenger",
	description = "Find the nearest free pickup location for common F2P items",
	tags = {"items", "navigation", "map", "f2p"}
)
public class ScavengerPlugin extends Plugin
{
	private static final int WORLD_MAP_MARKER_SIZE = 14;
	private static final int WORLD_MAP_ARROW_SIZE = 22;
	private static final BufferedImage WORLD_MAP_MARKER_HIDDEN = new BufferedImage(
		WORLD_MAP_MARKER_SIZE, WORLD_MAP_MARKER_SIZE, BufferedImage.TYPE_INT_ARGB);

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private WorldMapOverlay worldMapOverlay;

	@Inject
	private ScavengerPanel panel;

	@Inject
	private ScavengerMinimapOverlay minimapOverlay;

	@Inject
	private ScavengerWorldOverlay worldOverlay;

	@Inject
	private TargetManager targetManager;

	@Inject
	private ScavengerConfig config;

	private NavigationButton navButton;
	private WorldMapPoint worldMapPoint;
	private BufferedImage worldMapDotImage;

	@Override
	protected void startUp() throws Exception
	{
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Scavenger")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlayManager.add(minimapOverlay);
		overlayManager.add(worldOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		panel.shutdown();
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(worldOverlay);
		targetManager.clearActiveItem();
		if (worldMapPoint != null)
		{
			worldMapPointManager.remove(worldMapPoint);
			worldMapPoint = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		minimapOverlay.onGameTick();
		if (targetManager.getActiveItem() != null && client.getLocalPlayer() != null)
		{
			targetManager.refresh(client.getLocalPlayer().getWorldLocation());
		}
		updateWorldMapPoint();
	}

	// The dot image is recreated only when the tracked location changes (not
	// every tick), so a highlightColor config change won't retint it until the
	// target relocates - not worth the extra bookkeeping. Every tick we still
	// pick dot-vs-arrow based on the core's edge-snap state (one tick of lag,
	// since it's updated during render which runs after this) and swap in the
	// blank image to blink, matching the minimap arrow's cadence.
	private void updateWorldMapPoint()
	{
		NearestLocationFinder.Result result = targetManager.getActiveResult();
		if (result == null)
		{
			if (worldMapPoint != null)
			{
				worldMapPointManager.remove(worldMapPoint);
				worldMapPoint = null;
			}
			return;
		}

		SpawnLocation loc = result.location;
		WorldPoint worldPoint = new WorldPoint(loc.x, loc.y, loc.plane);
		if (worldMapPoint == null || !worldMapPoint.getWorldPoint().equals(worldPoint))
		{
			if (worldMapPoint != null)
			{
				worldMapPointManager.remove(worldMapPoint);
			}

			worldMapDotImage = dotImage(config.highlightColor());
			worldMapPoint = WorldMapPoint.builder()
				.worldPoint(worldPoint)
				.image(worldMapDotImage)
				.name(targetManager.getActiveItem().name)
				.snapToEdge(true)
				.jumpOnClick(true)
				.build();
			worldMapPointManager.add(worldMapPoint);
		}

		BufferedImage image = worldMapPoint.isCurrentlyEdgeSnapped()
			? arrowImage(config.highlightColor(), worldMapArrowAngle(worldPoint))
			: worldMapDotImage;
		worldMapPoint.setImage(minimapOverlay.isFlashOn() ? image : WORLD_MAP_MARKER_HIDDEN);
	}

	private double worldMapArrowAngle(WorldPoint worldPoint)
	{
		Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		Point drawPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(worldPoint);
		if (mapWidget == null || drawPoint == null)
		{
			return 0;
		}

		Rectangle bounds = mapWidget.getBounds();
		return Math.atan2(drawPoint.getY() - bounds.getCenterY(), drawPoint.getX() - bounds.getCenterX());
	}

	private static BufferedImage dotImage(Color color)
	{
		BufferedImage image = new BufferedImage(WORLD_MAP_MARKER_SIZE, WORLD_MAP_MARKER_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int inset = 1;
			int size = WORLD_MAP_MARKER_SIZE - 2 * inset;
			g.setColor(color);
			g.fillOval(inset, inset, size, size);
			g.setColor(Color.BLACK);
			g.drawOval(inset, inset, size, size);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static BufferedImage arrowImage(Color color, double angle)
	{
		BufferedImage image = new BufferedImage(WORLD_MAP_ARROW_SIZE, WORLD_MAP_ARROW_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			AffineTransform transform = new AffineTransform();
			transform.translate(WORLD_MAP_ARROW_SIZE / 2.0, WORLD_MAP_ARROW_SIZE / 2.0);
			transform.rotate(angle + Math.PI / 2);
			g.setTransform(transform);

			g.setColor(Color.BLACK);
			g.fillPolygon(arrowHead(WORLD_MAP_ARROW_SIZE - 2, WORLD_MAP_ARROW_SIZE - 6));
			g.setColor(color);
			g.fillPolygon(arrowHead(WORLD_MAP_ARROW_SIZE - 6, WORLD_MAP_ARROW_SIZE - 10));
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static Polygon arrowHead(int length, int width)
	{
		Polygon polygon = new Polygon();
		polygon.addPoint(0, -length / 2);
		polygon.addPoint(-width / 2, length / 2);
		polygon.addPoint(width / 2, length / 2);
		return polygon;
	}

	@Provides
	ScavengerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ScavengerConfig.class);
	}
}
