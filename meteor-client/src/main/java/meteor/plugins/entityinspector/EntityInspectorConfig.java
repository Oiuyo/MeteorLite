package meteor.plugins.entityinspector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import meteor.config.*;

@ConfigGroup("entityinspector")
public interface EntityInspectorConfig extends Config {
	@ConfigSection(
					keyName = "settings",
					position = 0,
					name = "Settings",
					description = "",
					closedByDefault = true
	)
	String displayedInfo = "Settings";

	@ConfigItem(
					keyName = "hover",
					name = "Hover only",
					description = "Show on hover",
					section = displayedInfo,
					position = 0
	)
	default boolean hover() {
		return true;
	}

	@ConfigItem(
					keyName = "ids",
					name = "IDs",
					description = "Show ids",
					section = displayedInfo,
					position = 1
	)
	default boolean ids() {
		return true;
	}

	@ConfigItem(
					keyName = "names",
					name = "Names",
					description = "Show names",
					section = displayedInfo,
					position = 2
	)
	default boolean names() {
		return true;
	}

	@ConfigItem(
					keyName = "actions",
					name = "Actions",
					description = "Show actions",
					section = displayedInfo,
					position = 3
	)
	default boolean actions() {
		return true;
	}

	@ConfigItem(
					keyName = "locations",
					name = "World locations",
					description = "Show world locations",
					section = displayedInfo,
					position = 4
	)
	default boolean worldLocations() {
		return true;
	}

	@ConfigItem(
					keyName = "indexes",
					name = "Indexes (Actors)",
					description = "Show indexes",
					section = displayedInfo,
					position = 5
	)
	default boolean indexes() {
		return true;
	}

	@ConfigItem(
					keyName = "animations",
					name = "Animations",
					description = "Show animations",
					section = displayedInfo,
					position = 6
	)
	default boolean animations() {
		return true;
	}

	@ConfigItem(
					keyName = "graphics",
					name = "Graphic (Actors)",
					description = "Show graphics",
					section = displayedInfo,
					position = 7
	)
	default boolean graphics() {
		return true;
	}

	@ConfigItem(
					keyName = "quantities",
					name = "Quantities (TileItems)",
					description = "Show quantities",
					section = displayedInfo,
					position = 8
	)
	default boolean quantities() {
		return true;
	}

	@ConfigSection(
					keyName = "tileObjects",
					position = 1,
					name = "Tile Objects",
					description = "",
					closedByDefault = true
	)
	String tileObjects = "Tile Objects";

	@ConfigItem(
					keyName = "gameObjects",
					name = "Game Objects",
					description = "Render Game Objects",
					section = tileObjects
	)
	default boolean gameObjects() {
		return false;
	}

	@ConfigItem(
					keyName = "decorObjects",
					name = "Decorative Objects",
					description = "Render Decorative Objects",
					section = tileObjects
	)
	default boolean decorObjects() {
		return false;
	}

	@ConfigItem(
					keyName = "wallObjects",
					name = "Wall Objects",
					description = "Render Wall Objects",
					section = tileObjects
	)
	default boolean wallObjects() {
		return false;
	}

	@ConfigItem(
					keyName = "groundObjects",
					name = "Ground Objects",
					description = "Render Ground Objects",
					section = tileObjects
	)
	default boolean groundObjects() {
		return false;
	}

	@ConfigItem(
					keyName = "tileItems",
					name = "TileItems",
					description = "Render Tile Items",
					section = tileObjects
	)
	default boolean tileItems() {
		return false;
	}

	@ConfigSection(
					keyName = "actors",
					name = "Actors",
					description = "",
					position = 2,
					closedByDefault = true
	)
	String actors = "Actors";

	@ConfigItem(
					keyName = "npcs",
					name = "NPCs",
					description = "Render NPCs",
					section = actors
	)
	default boolean npcs() {
		return false;
	}

	@ConfigItem(
					keyName = "players",
					name = "Players",
					description = "Render Players",
					section = actors
	)
	default boolean players() {
		return false;
	}

	@ConfigSection(
					name = "Others",
					keyName = "others",
					description = "",
					position = 9,
					closedByDefault = true
	)
	String others = "Others";

	@ConfigItem(
					keyName = "projectiles",
					name = "Projectiles",
					description = "Render Projectiles",
					section = others
	)
	default boolean projectiles() {
		return false;
	}

	@ConfigItem(
					keyName = "graphicsObjects",
					name = "Graphics Objects",
					description = "Render Graphics Objects",
					section = others
	)
	default boolean graphicsObjects() {
		return false;
	}

	@ConfigItem(
					keyName = "inventory",
					name = "Inventory",
					description = "Render Inventory",
					section = others
	)
	default boolean inventory() {
		return false;
	}

	@ConfigItem(
					keyName = "tileLocation",
					name = "Tile Location",
					description = "Render Tile Location",
					section = others
	)
	default boolean tileLocation() {
		return false;
	}

	@ConfigItem(
					keyName = "path",
					name = "Path",
					description = "Render calculated Path",
					position = 200,
					section = others
	)
	default boolean path() {
		return false;
	}

	@ConfigItem(
					keyName = "collisions",
					name = "Collision Map",
					description = "Render Collision Map",
					position = 201,
					section = others
	)
	default boolean collisions() {
		return false;
	}

	@ConfigItem(
					keyName = "packets",
					name = "Packets",
					description = "Packets",
					position = 202,
					section = others
	)
	default boolean packets() {
		return false;
	}

	@ConfigSection(
					name = "Utils",
					keyName = "utils",
					description = "",
					position = 10,
					closedByDefault = true
	)
	String utils = "Utils";

	@Icon(FontAwesomeIcon.SEARCH)
	@ConfigItem(
					keyName = "widgetInspector",
					name = "Widget Inspector",
					description = "Open the Widget Inspector",
					section = utils
	)
	default Button widgetInspector() {
		return new Button();
	}

	@Icon(FontAwesomeIcon.SEARCH)
	@ConfigItem(
					keyName = "varInspector",
					name = "Var Inspector",
					description = "Open the Var Inspector",
					section = utils
	)
	default Button varInspector() {
		return new Button();
	}

	@Icon(FontAwesomeIcon.SEARCH)
	@ConfigItem(
					keyName = "scriptInspector",
					name = "Script Inspector",
					description = "Open the Script Inspector",
					section = utils
	)
	default Button scriptInspector() {
		return new Button();
	}
}
