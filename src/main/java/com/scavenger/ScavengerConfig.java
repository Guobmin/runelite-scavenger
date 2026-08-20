package com.scavenger;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("scavenger")
public interface ScavengerConfig extends Config
{
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "Color used for the minimap arrow and tile highlight"
	)
	default Color highlightColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "accountType",
		name = "Account type",
		description = "Free-to-play hides items and locations that require membership"
	)
	default AccountType accountType()
	{
		return AccountType.MEMBERS;
	}
}
