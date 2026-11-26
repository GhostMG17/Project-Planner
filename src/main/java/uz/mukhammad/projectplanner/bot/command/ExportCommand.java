package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.export.records.ExportFile;
import uz.mukhammad.projectplanner.export.service.ExportService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExportCommand implements CommandHandler {

    private static final Role[] ALLOWED = {Role.ADMIN, Role.PORTFOLIO_LEAD, Role.PROJECT_MANAGER};

    private final AccessService accessService;
    private final ExportService exportService;
    private final MessageSender messageSender;

    @Override public String command() { return "/export"; }
    @Override public String description() {
        return "Excel export: /export CODE (project), /export (portfolio — Portfolio Lead/Admin)";
    }

    @Override
    public Set<Role> visibleTo() {
        return EnumSet.copyOf(Arrays.asList(ALLOWED));
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(actor, ALLOWED);
        String[] parts = message.getText().trim().split("\\s+");

        if (parts.length == 1) {
            ExportFile file = exportService.portfolioWorkbook(actor);
            messageSender.replyDocument(message, file, "📥 Portfolio export");
        } else if (parts.length == 2) {
            ExportFile file = exportService.projectWorkbook(actor, parts[1]);
            messageSender.replyDocument(message, file, "📥 Project export: " + parts[1].toUpperCase());
        } else {
            throw new BotException("Usage: /export CODE or /export");
        }
    }
}
