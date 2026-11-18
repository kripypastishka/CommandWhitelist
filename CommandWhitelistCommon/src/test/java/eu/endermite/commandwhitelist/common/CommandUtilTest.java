package eu.endermite.commandwhitelist.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CommandUtilTest {

    @Test
    void blockedSubcommandRequiresCompleteTokenBoundaries() {
        assertTrue(CommandUtil.matchesBlockedSubcommand("lp user info", "lp user"));
        assertTrue(CommandUtil.matchesBlockedSubcommand("/LP   USER  info", "lp user"));
        assertFalse(CommandUtil.matchesBlockedSubcommand("lp username", "lp user"));
        assertFalse(CommandUtil.matchesBlockedSubcommand("lpx user", "lp user"));
        assertFalse(CommandUtil.matchesBlockedSubcommand("lp", "lp user"));
    }

    @Test
    void rootCompletionsOnlyContainAllowedCommands() {
        assertEquals(
                List.of("help", "hello"),
                CommandUtil.filterRootCommandSuggestions(
                        "/he",
                        List.of("help", "heal", "hello"),
                        Set.of("help", "hello")
                )
        );
    }

    @Test
    void subcommandCompletionsHideBlockedNextToken() {
        assertEquals(
                List.of("group"),
                CommandUtil.filterSuggestions("/lp u", List.of("user", "group"), Set.of("lp user"))
        );
        assertEquals(
                List.of("permission"),
                CommandUtil.filterSuggestions("/lp user ", List.of("info", "permission"), Set.of("lp user info"))
        );
        assertTrue(CommandUtil.filterSuggestions(
                "/lp username ",
                List.of("info", "permission"),
                Set.of("lp user info")
        ).contains("info"));
    }

    @Test
    void noSuggestionsRemainAfterCompleteBlockedPath() {
        assertTrue(CommandUtil.filterSuggestions(
                "/lp user info ",
                List.of("name", "value"),
                Set.of("lp user info")
        ).isEmpty());
    }
}
