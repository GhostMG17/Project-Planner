package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.RiskView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.risk.records.RiskList;
import uz.mukhammad.projectplanner.risk.service.RiskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class RisksCommand implements CommandHandler {

    private final AccessService accessService;
    private final RiskService riskService;
    private final RiskView riskView;
    private final MessageSender messageSender;

    @Override public String command() { return "/risks"; }
    @Override public String description() { return "View project risks: /risks or /risks CODE"; }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        String[] parts = message.getText().trim().split("\\s+");

        if (parts.length == 1) {
            messageSender.reply(message, riskView.overview(riskService.findOpenVisible(actor)));
            return;
        }
        if (parts.length != 2) {
            throw new BotException("Usage: /risks or /risks CODE");
        }

        RiskList rl = riskService.getOpenRisks(actor, parts[1]);
        messageSender.reply(message, riskView.list(rl), riskView.listKeyboard(rl));
    }
}