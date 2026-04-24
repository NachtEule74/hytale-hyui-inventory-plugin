package com.nachteuele74.hytale.inventory.command;

import au.ellie.hyui.builders.ItemGridBuilder;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.ButtonBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nachteuele74.hytale.inventory.HytaleInventoryPlugin;
import com.nachteuele74.hytale.inventory.config.ConfigManager;
import com.nachteuele74.hytale.inventory.data.PlayerInventoryData;
import com.nachteuele74.hytale.inventory.data.PlayerInventoryDataManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

/**
 * Inventory command handler for displaying the 2-page inventory UI.
 */
public class InventoryCommand extends AbstractAsyncCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int SLOTS_PER_PAGE = 45;
    private static final int SLOTS_PER_ROW = 9;

    private final HytaleInventoryPlugin plugin;
    private final PlayerInventoryDataManager dataManager;
    private final ConfigManager configManager;

    public InventoryCommand(HytaleInventoryPlugin plugin, PlayerInventoryDataManager dataManager, ConfigManager configManager) {
        super("inventory", "Open your advanced inventory system");
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.setPermissionGroup(GameMode.Adventure);
        LOGGER.atInfo().log("InventoryCommand registered");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        var sender = commandContext.sender();
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        CompletableFuture<PlayerRef> playerRefFuture = new CompletableFuture<>();
        world.execute(() ->
                playerRefFuture.complete(store.getComponent(ref, PlayerRef.getComponentType()))
        );

        return playerRefFuture.thenComposeAsync(playerRef -> {
            if (playerRef == null) return CompletableFuture.completedFuture(null);
            String playerUUID = playerRef.getUuid().toString();

            return CompletableFuture.runAsync(() -> {
                PlayerInventoryData playerData = dataManager.getPlayerData(playerUUID);
                int unlockLevel = configManager.getConfig().getUnlockLevel();
                boolean page2Unlocked = playerData.getPlayerLevel() >= unlockLevel;

                if (page2Unlocked && !playerData.isPage2Unlocked()) {
                    playerData.unlockPage2();
                    dataManager.savePlayerData(playerUUID);
                }

                String page2Label = page2Unlocked ? "Seite 2" : "Seite 2 (Lvl " + unlockLevel + ")";
                String selectedTab = playerData.getCurrentPage() == 2 && page2Unlocked ? "page2" : "page1";

                String html = "<style>"
                        + ".container { anchor-width: 490; anchor-height: 320; }"
                        + "#inv-grid-1, #inv-grid-2 { anchor-height: 240; flex-weight: 1; }"
                        + "</style>"
                        + "<div class=\"page-overlay\">"
                        + "<div class=\"container\" data-hyui-title=\"Inventar\">"
                        + "<div class=\"container-contents\" style=\"layout-mode: Top; padding: 6;\">"
                        + "<nav id=\"inv-tabs\" class=\"tabs\""
                        + " data-tabs=\"page1:Seite 1,page2:" + page2Label + "\""
                        + " data-selected=\"" + selectedTab + "\">"
                        + "</nav>"
                        // Tab Seite 1
                        + "<div id=\"page1-content\" class=\"tab-content\" data-hyui-tab-id=\"page1\">"
                        + "<div id=\"inv-grid-1\" class=\"item-grid\""
                        + " data-hyui-slots-per-row=\"" + SLOTS_PER_ROW + "\""
                        + " data-hyui-are-items-draggable=\"true\">"
                        + "</div>"
                        + "</div>"
                        // Tab Seite 2
                        + "<div id=\"page2-content\" class=\"tab-content\" data-hyui-tab-id=\"page2\">"
                        + (page2Unlocked
                        ? "<div id=\"inv-grid-2\" class=\"item-grid\""
                        + " data-hyui-slots-per-row=\"" + SLOTS_PER_ROW + "\""
                        + " data-hyui-are-items-draggable=\"true\">"
                        + "</div>"
                        : "<p>Seite 2 wird bei Level " + unlockLevel + " freigeschaltet!</p>")
                        + "</div>"
                        + "</div>"
                        + "</div>"
                        + "</div>";

                PageBuilder page = PageBuilder.detachedPage()
                        .withLifetime(CustomPageLifetime.CanDismiss)
                        .fromHtml(html);

                // Seite 1 Grid befüllen
                page.getById("inv-grid-1", ItemGridBuilder.class).ifPresent(grid -> {
                    for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                        grid.addSlot(new ItemGridSlot());
                    }
                });

                // Seite 2 Grid befüllen (nur wenn freigeschaltet)
                if (page2Unlocked) {
                    page.getById("inv-grid-2", ItemGridBuilder.class).ifPresent(grid -> {
                        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                            grid.addSlot(new ItemGridSlot());
                        }
                    });
                }

                // Tab-Wechsel auf Seite 2 sperren wenn nicht freigeschaltet
                if (!page2Unlocked) {
                    page.addEventListener("inv-tabs", CustomUIEventBindingType.ValueChanged, event -> {
                        commandContext.sendMessage(Message.raw(
                                "§e[Inventar] §cSeite 2 wird bei Level " + unlockLevel + " freigeschaltet!"
                        ));
                    });
                } else {
                    // Tab-Wechsel speichern
                    page.addEventListener("inv-tabs", CustomUIEventBindingType.ValueChanged, event -> {
                        // Aktuell keine direkte Möglichkeit den Tab-Wert aus dem Event zu lesen
                        // Wird beim nächsten Öffnen des Inventars gespeichert
                    });
                }

                page.open(playerRef, store);
            }, world);
        });
    }
}