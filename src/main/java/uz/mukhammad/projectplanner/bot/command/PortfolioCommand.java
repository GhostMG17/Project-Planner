package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.ReportView;
import uz.mukhammad.projectplanner.report.PortfolioService.PortfolioService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PortfolioCommand implements CommandHandler {

    private static final Role[] ALLOWED = {Role.ADMIN, Role.PORTFOLIO_LEAD};

    private final AccessService accessService;
    private final PortfolioService portfolioService;
    private final ReportView reportView;
    private final MessageSender messageSender;

    @Override public String command() { return "/portfolio"; }
    @Override public String description() { return "Portfolio overview of all projects"; }

    @Override
    public Set<Role> visibleTo() {
        return EnumSet.copyOf(Arrays.asList(ALLOWED));
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(actor, ALLOWED);
        messageSender.replyLong(message, reportView.portfolio(portfolioService.build(actor)), null);
    }
}
