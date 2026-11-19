package uz.mukhammad.projectplanner.bot.conversation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.command.CallbackHandler;
import uz.mukhammad.projectplanner.bot.view.RiskView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.risk.entity.Risk;
import uz.mukhammad.projectplanner.risk.enums.RiskSeverity;
import uz.mukhammad.projectplanner.risk.records.RiskList;
import uz.mukhammad.projectplanner.risk.service.RiskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskFlow implements CallbackHandler, InputHandler {

    public static final String NAME = "risk_wizard";

    enum Step { DESCRIPTION, SEVERITY, OWNER }

    private final AccessService accessService;
    private final RiskService riskService;
    private final ProjectService projectService;
    private final RiskView riskView;
    private final MessageSender messageSender;
    private final ConversationStore conversationStore;

    @Override
    public String prefix() {
        return RiskView.PREFIX;
    }

    @Override
    public String name() {
        return NAME;
    }

    // ===================== Кнопки =====================

    @Override
    public void handle(CallbackQuery cq) {
        User actor = accessService.requireUser(cq.getFrom().getId());
        Long chatId = cq.getMessage().getChatId();
        Integer messageId = cq.getMessage().getMessageId();
        ConversationKey key = new ConversationKey(chatId, cq.getFrom().getId());

        String[] parts = cq.getData().split(":", 3);
        if (parts.length != 3) {
            throw new BotException("Unknown action.");
        }
        String action = parts[1];
        String value = parts[2];

        switch (action) {
            case "list" -> {
                conversationStore.clear(key);
                RiskList rl = riskService.getOpenRisks(actor, parseLong(value));
                messageSender.edit(chatId, messageId, riskView.list(rl), riskView.listKeyboard(rl));
            }
            case "add" -> {
                Project p = riskService.prepareAdd(actor, parseLong(value));
                conversationStore.start(key, NAME, Step.DESCRIPTION.name(), p.getId());
                messageSender.edit(chatId, messageId, riskView.descriptionPrompt(p), riskView.cancelKeyboard());
            }
            case "close" -> {
                Risk risk = riskService.closeRisk(actor, parseLong(value));
                Project p = risk.getProject();
                messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                        "✅ Risk closed by " + actor.getShortName() + "\n" + risk.getDescription());
                RiskList rl = riskService.getOpenRisks(actor, p.getId());
                messageSender.edit(chatId, messageId, riskView.list(rl), riskView.listKeyboard(rl));
            }
            case "sev" -> {
                ConversationState state = requireState(key, Step.SEVERITY);
                state.data().put("severity", RiskSeverity.valueOf(value).name());
                conversationStore.advance(key, Step.OWNER.name());
                Project p = projectService.getById(state.entityId());
                messageSender.edit(chatId, messageId, riskView.ownerPrompt(),
                        riskView.ownerKeyboard(projectService.getTeamUsers(actor, p), actor));
            }
            case "own" -> {
                ConversationState state = requireState(key, Step.OWNER);
                Risk risk = riskService.createRisk(actor, state.entityId(),
                        state.data().get("description"),
                        RiskSeverity.valueOf(state.data().get("severity")),
                        parseLong(value));
                conversationStore.clear(key);

                RiskList rl = riskService.getOpenRisks(actor, state.entityId());
                messageSender.edit(chatId, messageId, "✅ Risk added.\n\n" + riskView.list(rl), riskView.listKeyboard(rl));
                notifyCreated(risk, actor);
            }
            case "cancel" -> {
                conversationStore.clear(key);
                messageSender.edit(chatId, messageId, "✖ Cancelled.", null);
            }
            default -> throw new BotException("Unknown action.");
        }
    }

    // ===================== Текст (шаг 1) =====================

    @Override
    public void handle(Message message, ConversationState state) {
        if (Step.valueOf(state.step()) != Step.DESCRIPTION) {
            throw new BotException("Please use the buttons above.");
        }
        if (!message.hasText()) {
            throw new BotException("Please describe the risk in text.");
        }
        String text = message.getText().trim();
        if (text.isEmpty() || text.length() > 500) {
            throw new BotException("Risk description must be 1–500 characters.");
        }

        state.data().put("description", text);
        conversationStore.advance(new ConversationKey(message.getChatId(), message.getFrom().getId()),
                Step.SEVERITY.name());
        messageSender.reply(message, riskView.severityPrompt(), riskView.severityKeyboard());
    }

    // ===================== helpers =====================

    private void notifyCreated(Risk risk, User actor) {
        Project p = risk.getProject();
        RiskSeverity s = risk.getSeverity();
        String text = "⚠️ New risk in " + p.getCode() + " · " + s.getIcon() + " " + s.getLabel() + "\n"
                + risk.getDescription() + "\n\n"
                + "Owner: " + risk.getOwner().getShortName() + "\n"
                + "Raised by: " + actor.getShortName();

        messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(), text);

        User pm = p.getProjectManager();
        boolean serious = s == RiskSeverity.HIGH || s == RiskSeverity.CRITICAL;
        if (serious && !pm.getId().equals(actor.getId())) {
            messageSender.send(pm.getTelegramId(), null, "📬 " + text);
        }
    }

    private ConversationState requireState(ConversationKey key, Step expected) {
        ConversationState state = conversationStore.get(key)
                .filter(s -> NAME.equals(s.handler()))
                .orElseThrow(() -> new BotException(
                        "This form has expired or belongs to someone else. Open /risks again."));
        if (Step.valueOf(state.step()) != expected) {
            throw new BotException("This button is outdated. Please use the latest message.");
        }
        return state;
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new BotException("Unknown action.");
        }
    }
}
