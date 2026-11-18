package eu.endermite.commandwhitelist.bukkit.listeners.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import eu.endermite.commandwhitelist.bukkit.CommandWhitelistBukkit;
import eu.endermite.commandwhitelist.common.CWPermission;
import eu.endermite.commandwhitelist.common.CommandUtil;
import eu.endermite.commandwhitelist.common.ConfigCache;
import eu.endermite.commandwhitelist.common.commands.CWCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Locale;

@Deprecated(forRemoval = true)
public class PacketCommandPreProcessListener {

    public static void protocol(CommandWhitelistBukkit plugin) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        commandExecListener(protocolManager, plugin);
    }

    private static boolean matchesBlockedSubcommand(String command, String blockedPath) {
        String normalizedCommand = command.trim().toLowerCase(Locale.ROOT);
        String normalizedBlocked = blockedPath.trim().toLowerCase(Locale.ROOT);
        if (normalizedCommand.startsWith("/")) normalizedCommand = normalizedCommand.substring(1);
        if (normalizedBlocked.startsWith("/")) normalizedBlocked = normalizedBlocked.substring(1);

        String[] commandTokens = normalizedCommand.isEmpty() ? new String[0] : normalizedCommand.split("\\s+");
        String[] blockedTokens = normalizedBlocked.isEmpty() ? new String[0] : normalizedBlocked.split("\\s+");
        if (blockedTokens.length == 0 || commandTokens.length < blockedTokens.length) return false;

        for (int i = 0; i < blockedTokens.length; i++) {
            if (!commandTokens[i].equals(blockedTokens[i])) return false;
        }
        return true;
    }

    public static void commandExecListener(ProtocolManager protocolManager, Plugin plugin) {
        protocolManager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.CHAT,
                PacketType.Play.Client.CHAT_COMMAND,
                PacketType.Play.Client.CHAT_COMMAND_SIGNED
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketType packetType = event.getPacketType();
                PacketContainer packet = event.getPacket();
                String input = packet.getStrings().read(0);
                if (input == null || input.isBlank()) return;

                // Legacy chat packets carry the leading slash; command packets carry the command itself.
                if (packetType.equals(PacketType.Play.Client.CHAT)) {
                    if (!input.startsWith("/")) return;
                    input = input.substring(1);
                } else if (input.startsWith("/")) {
                    input = input.substring(1);
                }

                String command = input.trim();
                if (command.isEmpty()) return;

                Player player = event.getPlayer();
                if (player.hasPermission(CWPermission.BYPASS.permission())) return;

                ConfigCache config = CommandWhitelistBukkit.getConfigCache();
                String label = CommandUtil.getCommandLabel(command).toLowerCase(Locale.ROOT);
                HashSet<String> commands = CommandWhitelistBukkit.getCommands(player);
                BukkitAudiences audiences = CommandWhitelistBukkit.getAudiences();

                if (!commands.contains(label)) {
                    event.setCancelled(true);
                    audiences.player(player).sendMessage(
                            CWCommand.miniMessage.deserialize(
                                    config.prefix + CommandWhitelistBukkit.getCommandDeniedMessage(label)
                            )
                    );
                    return;
                }

                for (String bannedSubCommand : CommandWhitelistBukkit.getSuggestions(player)) {
                    if (matchesBlockedSubcommand(command, bannedSubCommand)) {
                        event.setCancelled(true);
                        audiences.player(player).sendMessage(
                                CWCommand.miniMessage.deserialize(config.prefix + config.subcommand_denied)
                        );
                        return;
                    }
                }
            }
        });
    }
}
