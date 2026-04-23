package com.nachteuele74.hytale.inventory.command;

import au.ellie.hyui.builders.PageBuilder;
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
    
    private final HytaleInventoryPlugin plugin;
    private final PlayerInventoryDataManager dataManager;
    private final ConfigManager configManager;

    public InventoryCommand(HytaleInventoryPlugin plugin, PlayerInventoryDataManager dataManager, ConfigManager configManager) {
        super("inventory", "Open your advanced inventory system");
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.setPermissionGroup(GameMode.Adventure); // Allow all players
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
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        String playerUUID = player.getGameProfile().getId().toString();

        return CompletableFuture.runAsync(() -> {
            if (playerRef == null) return;

            // Load player data
            PlayerInventoryData playerData = dataManager.getPlayerData(playerUUID);
            int unlockLevel = configManager.getConfig().getUnlockLevel();
            boolean page2Unlocked = playerData.getPlayerLevel() >= unlockLevel;

            if (page2Unlocked && !playerData.isPage2Unlocked()) {
                playerData.unlockPage2();
                dataManager.savePlayerData(playerUUID);
            }

            // Build the UI
            PageBuilder page = PageBuilder.detachedPage()
                    .withLifetime(CustomPageLifetime.CanDismiss)
                    .fromHtml(buildInventoryHTML(playerData, page2Unlocked, unlockLevel));

            // Setup page 1 button
            page.getById("page1Btn", au.ellie.hyui.builders.ButtonBuilder.class).ifPresent(button -> {
                button.addEventListener(CustomUIEventBindingType.Activating, event -> {
                    playerData.setCurrentPage(1);
                    dataManager.savePlayerData(playerUUID);
                    commandContext.sendMessage(Message.raw("§e[Inventory] §7Switched to Page 1"));
                });
            });

            // Setup page 2 button
            page.getById("page2Btn", au.ellie.hyui.builders.ButtonBuilder.class).ifPresent(button -> {
                if (page2Unlocked) {
                    button.addEventListener(CustomUIEventBindingType.Activating, event -> {
                        playerData.setCurrentPage(2);
                        dataManager.savePlayerData(playerUUID);
                        commandContext.sendMessage(Message.raw("§e[Inventory] §7Switched to Page 2"));
                    });
                } else {
                    commandContext.sendMessage(Message.raw(
                            "§e[Inventory] §cPage 2 unlocked at Level " + unlockLevel + "!"
                    ));
                }
            });

            // Open the UI
            page.open(playerRef, store);
        }, world);
    }

    /**
     * Builds the HyUIML HTML for the inventory UI.
     */
    private String buildInventoryHTML(PlayerInventoryData playerData, boolean page2Unlocked, int unlockLevel) {
        String page2Badge = page2Unlocked ? "" : "🔒";
        String page2Disabled = page2Unlocked ? "" : " style=\"opacity: 0.5;\"";

        return """
                <div class="page-overlay">
                    <div class="container" data-hyui-title="Advanced Inventory">
                        <div style="display: flex; gap: 10px; margin-bottom: 15px;">
                            <button id="page1Btn" style="flex: 1; background-color: #4CAF50; padding: 10px; border-radius: 5px;">
                                <b>Page 1</b>
                            </button>
                            <button id="page2Btn" style="flex: 1; background-color: #2196F3; padding: 10px; border-radius: 5px;""" + page2Disabled + """
                                <b>Page 2</b> """ + page2Badge + """
                            </button>
                        </div>
                        
                        <div style="padding: 10px; background-color: #f0f0f0; border-radius: 5px; margin-bottom: 10px;">
                            <p><b>Player Level:</b> """ + playerData.getPlayerLevel() + """</p>
                            """ + (page2Unlocked ? "<p style=\"color: green;\"><b>✓ Page 2 Unlocked</b></p>" 
                                    : "<p style=\"color: red;\"><b>✗ Unlock at Level " + unlockLevel + "</b></p>") + """
                        </div>
                        
                        """ + (playerData.getCurrentPage() == 1 ? 
                            "<div style=\"background-color: #e8f5e9; padding: 10px; border-radius: 5px;\">"
                            + "<p><b>Page 1 Items</b></p>"
                            + "<p>Slot 1-20 available</p>"
                            + "</div>"
                        : 
                            "<div style=\"background-color: #e3f2fd; padding: 10px; border-radius: 5px;\">"
                            + "<p><b>Page 2 Items</b></p>"
                            + (page2Unlocked ? "<p>Slot 21-40 available</p>" : "<p>🔒 Locked until Level " + unlockLevel + "</p>")
                            + "</div>"
                        ) + """
                    </div>
                </div>
                """;
    }
}