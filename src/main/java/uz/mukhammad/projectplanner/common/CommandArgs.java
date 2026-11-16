package uz.mukhammad.projectplanner.common;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Arrays;

public final class CommandArgs {

    private CommandArgs() {}

    /** "/addmember UTY @john" + expected=2 → ["UTY", "@john"] */
    public static String[] parse(Message message, int expected, String usage) {
        String[] parts = message.getText().trim().split("\\s+");
        if (parts.length != expected + 1) {
            throw new BotException(usage);
        }
        return Arrays.copyOfRange(parts, 1, parts.length);
    }
}
