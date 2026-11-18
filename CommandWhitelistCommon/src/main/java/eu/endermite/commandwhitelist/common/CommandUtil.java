package eu.endermite.commandwhitelist.common;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class CommandUtil {

    /**
     * Filters blocked command suggestions from provided collection of strings.
     * Suggestions are matched by complete command tokens, not raw string prefixes.
     *
     * @param buffer             Command buffer
     * @param suggestions        Full suggestions list
     * @param blockedSubCommands Subcommands to filter out
     * @return Filtered list of suggestions
     */
    public static List<String> filterSuggestions(String buffer, Collection<String> suggestions, Collection<String> blockedSubCommands) {
        List<String> filtered = new ArrayList<>(suggestions);
        if (filtered.isEmpty() || blockedSubCommands.isEmpty()) return filtered;

        String normalizedBuffer = normalizeCommand(buffer);
        boolean endsWithWhitespace = !normalizedBuffer.isEmpty()
                && Character.isWhitespace(normalizedBuffer.charAt(normalizedBuffer.length() - 1));
        String trimmedBuffer = normalizedBuffer.trim();
        String[] inputTokens = trimmedBuffer.isEmpty() ? new String[0] : trimmedBuffer.split("\\s+");
        int completedTokenCount = endsWithWhitespace ? inputTokens.length : Math.max(0, inputTokens.length - 1);
        String partialToken = endsWithWhitespace || inputTokens.length == 0
                ? ""
                : inputTokens[inputTokens.length - 1];

        for (String blockedSubCommand : blockedSubCommands) {
            String[] blockedTokens = tokenize(blockedSubCommand);
            if (blockedTokens.length == 0 || completedTokenCount > blockedTokens.length) continue;
            if (!tokensEqual(inputTokens, 0, blockedTokens, 0, completedTokenCount)) continue;

            int nextTokenIndex = completedTokenCount;
            String partial = partialToken;
            if (!endsWithWhitespace
                    && nextTokenIndex < blockedTokens.length
                    && blockedTokens[nextTokenIndex].equals(partial)) {
                nextTokenIndex++;
                partial = "";
            }

            // Once the complete blocked path has been entered, do not offer further completions.
            if (nextTokenIndex >= blockedTokens.length) return List.of();

            String blockedNextToken = blockedTokens[nextTokenIndex];
            if (partial.isEmpty() || blockedNextToken.startsWith(partial)) {
                filtered.removeIf(suggestion ->
                        normalizeCommand(suggestion).trim().equals(blockedNextToken));
            }
        }
        return filtered;
    }

    /**
     * Returns true when the command begins with the complete token sequence in blockedPath.
     * For example, "lp user info" matches "lp user", but "lp username" does not.
     */
    public static boolean matchesBlockedSubcommand(String command, String blockedPath) {
        String[] commandTokens = tokenize(command);
        String[] blockedTokens = tokenize(blockedPath);
        if (blockedTokens.length == 0 || commandTokens.length < blockedTokens.length) return false;
        return tokensEqual(commandTokens, 0, blockedTokens, 0, blockedTokens.length);
    }

    /**
     * Filters root command completions against the configured allow-list.
     */
    public static List<String> filterRootCommandSuggestions(
            String buffer,
            Collection<String> suggestions,
            Collection<String> allowedCommands
    ) {
        List<String> filtered = new ArrayList<>();
        if (!buffer.startsWith("/")) return new ArrayList<>(suggestions);

        String partial = normalizeCommand(buffer.substring(1)).trim();
        if (partial.contains(" ")) return new ArrayList<>(suggestions);

        for (String suggestion : suggestions) {
            String candidate = normalizeCommand(suggestion);
            if (!candidate.isEmpty()
                    && !candidate.contains(" ")
                    && candidate.startsWith(partial)
                    && allowedCommands.stream().map(CommandUtil::normalizeCommand)
                    .map(String::trim).anyMatch(candidate::equals)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    private static String[] tokenize(String command) {
        String normalized = normalizeCommand(command).trim();
        return normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
    }

    private static String normalizeCommand(String command) {
        String normalized = command == null ? "" : command.stripLeading();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static boolean tokensEqual(
            String[] left,
            int leftOffset,
            String[] right,
            int rightOffset,
            int length
    ) {
        if (leftOffset + length > left.length || rightOffset + length > right.length) return false;
        for (int i = 0; i < length; i++) {
            if (!left[leftOffset + i].equals(right[rightOffset + i])) return false;
        }
        return true;
    }

    /**
     * @param cmd The command
     * @return Last argument of the command
     */
    public static String getLastArgument(String cmd) {
        String[] parts = cmd.split(" ");
        if (parts.length == 0) return "";
        return parts[parts.length - 1];
    }

    /**
     * @param cmd The command
     * @return Command without the last argument.
     */
    public static String cutLastArgument(String cmd) {
        String[] cmdSplit = cmd.split(" ");
        StringBuilder cmdBuilder = new StringBuilder();
        for (int i = 0; i <= cmdSplit.length - 2; i++)
            cmdBuilder.append(cmdSplit[i]).append(" ");
        return cmdBuilder.toString();
    }

    /**
     * @param cmd The command
     * @return Command label
     */
    public static String getCommandLabel(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        if (parts.length == 0) return "";
        if (parts[0].startsWith("/"))
            parts[0] = parts[0].substring(1);
        return parts[0];
    }

    /**
     * Dumps command list to a file
     *
     * @param serverCommands Commands to dump
     * @return True on successful file save
     */
    public static boolean dumpAllBukkitCommands(ArrayList<String> serverCommands, File file) {
        try {
            File parent = new File(file.getParent());
            if (!parent.exists())
                parent.mkdir();
            if (!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            return false;
        }

        try {
            ConfigFile dumpFile = ConfigFile.loadConfig(file);
            dumpFile.set("commands", serverCommands);
            dumpFile.save();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
