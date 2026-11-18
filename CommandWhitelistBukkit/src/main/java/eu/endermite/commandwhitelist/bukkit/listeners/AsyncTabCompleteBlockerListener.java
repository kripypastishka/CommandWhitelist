package eu.endermite.commandwhitelist.bukkit.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import eu.endermite.commandwhitelist.bukkit.CommandWhitelistBukkit;
import eu.endermite.commandwhitelist.common.CWPermission;
import eu.endermite.commandwhitelist.common.CommandUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AsyncTabCompleteBlockerListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandTabComplete(AsyncTabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (player.hasPermission(CWPermission.BYPASS.permission())) return;
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;

        int commandEnd = buffer.indexOf(' ');
        if (commandEnd < 0) {
            event.setCompletions(CommandUtil.filterRootCommandSuggestions(
                    buffer,
                    event.getCompletions(),
                    CommandWhitelistBukkit.getCommands(player)
            ));
            return;
        }

        String commandLabel = buffer.substring(1, commandEnd).toLowerCase(java.util.Locale.ROOT);
        if (!CommandWhitelistBukkit.getCommands(player).contains(commandLabel)) {
            CommandWhitelistBukkit.getConfigCache().debug("Actively prevented " + event.getSender().getName() + "'s tab completion (disallowed command)");
            event.setCancelled(true);
            return;
        }
        if (event.getCompletions().isEmpty()) {
            return;
        }
        event.setCompletions(CommandUtil.filterSuggestions(buffer, event.getCompletions(), CommandWhitelistBukkit.getSuggestions(player)));
    }

}
