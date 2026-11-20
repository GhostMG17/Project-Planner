package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.ReportView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.report.records.ProjectReport;
import uz.mukhammad.projectplanner.report.service.ReportService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class ReportCallbackHandler implements CallbackHandler {

    private final AccessService accessService;
    private final ReportService reportService;
    private final ReportView reportView;
    private final MessageSender messageSender;

    @Override
    public String prefix() {
        return ReportView.PREFIX;
    }

    @Override
    public void handle(CallbackQuery cq) {
        String[] parts = cq.getData().split(":");
        if (parts.length != 3 || !"proj".equals(parts[1])) {
            throw new BotException("Unknown action.");
        }
        User actor = accessService.requireUser(cq.getFrom().getId());
        Long projectId;
        try {
            projectId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new BotException("Unknown action.");
        }

        ProjectReport report = reportService.projectReport(actor, projectId);

        Integer threadId = cq.getMessage() instanceof Message m && m.isTopicMessage()
                ? m.getMessageThreadId() : null;
        messageSender.sendLong(cq.getMessage().getChatId(), threadId, reportView.projectReport(report), null);
    }
}
