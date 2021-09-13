package meteor.plugins.api.example.simpleoneclick;

import com.google.inject.Inject;
import com.google.inject.Provides;
import meteor.config.ConfigManager;
import meteor.eventbus.Subscribe;
import meteor.eventbus.events.ConfigChanged;
import meteor.plugins.Plugin;
import meteor.plugins.PluginDescriptor;
import meteor.plugins.api.entities.NPCs;
import meteor.plugins.api.entities.Players;
import meteor.plugins.api.entities.TileItems;
import meteor.plugins.api.entities.TileObjects;
import meteor.plugins.api.items.Bank;
import meteor.plugins.api.items.Inventory;
import meteor.plugins.api.widgets.Widgets;
import meteor.util.Text;
import net.runelite.api.*;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PluginDescriptor(
				name = "Hoot One Click"
)
public class HootOneClickPlugin extends Plugin {
	@Inject
	private HootOneClickConfig config;

	private static final List<Integer> GAME_OBJECT_OPCODES = List.of(1, 2, 3, 4, 5, 6, 1001);
	private static final List<Integer> NPC_OPCODES = List.of(7, 8, 9, 10, 11, 12, 13);
	private static final List<Integer> GROUND_ITEM_OPCODES = List.of(18, 19, 20, 21, 22);
	private static final List<Integer> WIDGET_OPCODES = List.of(24, 25, 26, 28, 29, 30, 39, 40, 41, 42, 43);
	private static final List<Integer> ITEM_OPCODES = List.of(33, 34, 35, 36, 37, 38);
	private static final List<Integer> PLAYER_OPCODES = List.of(44, 45, 46, 47, 48, 49, 50, 51);

	private final Map<String, String> gameObjectConfigs = new HashMap<>();
	private final Map<String, String> npcConfigs = new HashMap<>();
	private final Map<String, String> groundItemConfigs = new HashMap<>();
	private final Map<String, String> widgetConfigs = new HashMap<>();
	private final Map<String, String> itemConfigs = new HashMap<>();
	private final Map<String, String> playerConfigs = new HashMap<>();

	@Provides
	public HootOneClickConfig getConfig(ConfigManager configManager) {
		return configManager.getConfig(HootOneClickConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e) {
		if (!e.getGroup().equals("hootoneclick")) {
			return;
		}

		clearConfigs();

		parseConfigs(config.gameObjectConfig(), gameObjectConfigs);
		parseConfigs(config.npcConfig(), npcConfigs);
		parseConfigs(config.groundItemConfig(), groundItemConfigs);
		parseConfigs(config.widgetConfig(), widgetConfigs);
		parseConfigs(config.itemConfig(), itemConfigs);
		parseConfigs(config.playerConfig(), playerConfigs);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e) {
		int opcode = e.getMenuAction().getId();

		if (!gameObjectConfigs.isEmpty() && GAME_OBJECT_OPCODES.contains(opcode)) {
			TileObject obj = TileObjects.getNearest(e.getId());
			if (obj != null && gameObjectConfigs.containsKey(obj.getName())) {
				String replaced = gameObjectConfigs.get(obj.getName());
				e.consume();
				if (isUseOn(replaced)) {
					Item usedItem = getUsedItem(replaced);
					if (usedItem != null) {
						usedItem.useOn(obj);
					}

					return;
				}

				obj.interact(replaced);
			}

			return;
		}

		if (!npcConfigs.isEmpty() && NPC_OPCODES.contains(opcode)) {
			NPC npc = NPCs.getNearest(x -> x.getIndex() == e.getId());
			if (npc != null && npcConfigs.containsKey(npc.getName())) {
				String replaced = npcConfigs.get(npc.getName());
				e.consume();
				if (isUseOn(replaced)) {
					Item usedItem = getUsedItem(replaced);
					if (usedItem != null) {
						usedItem.useOn(npc);
					}

					return;
				}

				npc.interact(replaced);
			}

			return;
		}

		if (!groundItemConfigs.isEmpty() && GROUND_ITEM_OPCODES.contains(opcode)) {
			TileItem item = TileItems.getNearest(e.getId());
			if (item != null && groundItemConfigs.containsKey(item.getName())) {
				String replaced = groundItemConfigs.get(item.getName());
				e.consume();
				if (isUseOn(replaced)) {
					Item usedItem = getUsedItem(replaced);
					if (usedItem != null) {
						usedItem.useOn(item);
					}

					return;
				}

				item.interact(replaced);
			}

			return;
		}

		if (!widgetConfigs.isEmpty() && WIDGET_OPCODES.contains(opcode)) {
			String action = Text.removeTags(e.getMenuOption()) + " " + Text.removeTags(e.getMenuTarget());
			logger.debug(action);
			Widget widget = Widgets.fromId(e.getParam1());
			if (widget != null && widgetConfigs.containsKey(action)) {
				String replaced = widgetConfigs.get(action);
				e.consume();
				if (isUseOn(replaced)) {
					Item usedItem = getUsedItem(replaced);
					if (usedItem != null) {
						usedItem.useOn(widget);
					}

					return;
				}

				widget.interact(replaced);
			}

			return;
		}

		if (!itemConfigs.isEmpty() && ITEM_OPCODES.contains(opcode)) {
			Item item = Inventory.getFirst(e.getId());

			System.out.println(item);

			if (item != null && itemConfigs.containsKey(item.getName())) {
				logger.debug(item.getName());

				String replaced = itemConfigs.get(item.getName());
				e.consume();
				if (isUseOn(replaced)) {
					Item usedItem = getUsedItem(replaced);
					if (usedItem != null) {
						usedItem.useOn(item);
					}

					return;
				}

				item.interact(replaced);
			}

			return;
		}

		if (!playerConfigs.isEmpty() && PLAYER_OPCODES.contains(opcode)) {
			Player player = Players.getNearest(x -> x.getIndex() == e.getId());
			if (player != null && playerConfigs.containsKey(player.getName())) {
				String replaced = playerConfigs.get(player.getName());
				e.consume();
				if (isUseOn(replaced)) {
					Item usedItem = getUsedItem(replaced);
					if (usedItem != null) {
						usedItem.useOn(player);
					}

					return;
				}

				player.interact(replaced);
			}
		}
	}

	private Item getUsedItem(String replacement) {
		return Inventory.getFirst(replacement.substring(4));
	}

	private boolean isUseOn(String action) {
		return action.contains("Use ") && action.split(" ").length >= 2;
	}

	private void parseConfigs(String text, Map<String, String> configs) {
		String[] items = text.split(",");
		for (String i : items) {
			String[] pairs = i.split(":");
			configs.put(pairs[0], pairs[1]);
		}
	}

	private void clearConfigs() {
		gameObjectConfigs.clear();
		npcConfigs.clear();
		groundItemConfigs.clear();
		widgetConfigs.clear();
		itemConfigs.clear();
		playerConfigs.clear();
	}
}