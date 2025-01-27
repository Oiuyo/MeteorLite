package meteor.plugins.cettitutorial.tasks;

import meteor.PluginTask;
import meteor.plugins.api.entities.NPCs;
import meteor.plugins.api.entities.Players;
import meteor.plugins.api.entities.TileObjects;
import meteor.plugins.api.game.Game;
import meteor.plugins.api.items.Inventory;
import meteor.plugins.api.widgets.Dialog;
import meteor.plugins.api.widgets.Tab;
import meteor.plugins.api.widgets.Tabs;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;

import static osrs.Client.logger;

public class QuestGuide implements PluginTask {

	@Override
	public boolean validate() {
		return Game.getClient().getVarpValue(281) < 260;
	}

	private void talkToGuide() {
		if (Dialog.canContinue()) {
			Dialog.continueSpace();
			return;
		}

		if (!Players.getLocal().isIdle()) {
			return;
		}

		NPC guide = NPCs.getNearest("Quest Guide");
		if (guide == null) {
			return;
		}

		guide.interact("Talk-to");
	}

	private void enterArea() {
		if (Players.getLocal().isMoving()) {
			return;
		}

		TileObject door = TileObjects.getNearest(9716);
		if (door == null) {
			return;
		}

		door.interact(0);
	}

	private void leaveArea() {
		if (!Players.getLocal().isIdle()) {
			return;
		}

		TileObject ladder = TileObjects.getNearest(9726);
		if (ladder == null) {
			return;
		}

		ladder.interact(0);
	}

	private void openQuests() {
		if (Dialog.canContinue()) {
			Dialog.continueSpace();
		}

		Widget quest = Game.getClient().getWidget(164, 53);

		if (quest == null) {
			return;
		}

		quest.interact("Quest List");
	}

	@Override
	public int execute() {
		switch (Game.getClient().getVarpValue(281)) {
			case 200 -> enterArea();
			case 220, 240 -> talkToGuide();
			case 230 -> openQuests();
			case 250 -> leaveArea();
		}
		return 700;
	}
}
