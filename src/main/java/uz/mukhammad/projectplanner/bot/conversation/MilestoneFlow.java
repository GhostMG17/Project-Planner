package uz.mukhammad.projectplanner.bot.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.command.CallbackHandler;
import uz.mukhammad.projectplanner.bot.view.MilestoneView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.milestone.service.MilestoneService;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MilestoneFlow implements CallbackHandler, InputHandler {

    public static final String NAME = "milestone_wizard";

    enum Step { NAME, DATE }

    private final AccessService accessService;
    private final MilestoneService milestoneService;
    private final ProjectService projectService;
    private final MilestoneView milestoneView;
    private final MessageSender messageSender;
    private final ConversationStore conversationStore;
    private final AppProperties appProperties;

    @Override
    public String prefix() {
        return MilestoneView.PREFIX;
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
                MilestoneList ml = milestoneService.getMilestones(actor, parseLong(value));
                messageSender.edit(chatId, messageId, milestoneView.list(ml), milestoneView.listKeyboard(ml));
            }
            case "add" -> {
                Project p = milestoneService.prepareAdd(actor, parseLong(value));
                conversationStore.start(key, NAME, Step.NAME.name(), p.getId());
                messageSender.edit(chatId, messageId, milestoneView.namePrompt(p), milestoneView.cancelKeyboard());
            }
            case "toggle" -> {
                Milestone m = milestoneService.toggle(actor, parseLong(value));
                Project p = m.getProject();
                if (m.getStatus() == MilestoneStatus.COMPLETED) {
                    messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                            "🎯 Milestone reached: " + m.getName() + "\n(marked by " + actor.getShortName() + ")");
                }
                MilestoneList ml = milestoneService.getMilestones(actor, p.getId());
                messageSender.edit(chatId, messageId, milestoneView.list(ml), milestoneView.listKeyboard(ml));
            }
            case "date" -> {
                ConversationState state = requireState(key, Step.DATE);
                Milestone m = save(actor, state, LocalDate.parse(value), key);
                MilestoneList ml = milestoneService.getMilestones(actor, m.getProject().getId());
                messageSender.edit(chatId, messageId,
                        "✅ Milestone added.\n\n" + milestoneView.list(ml), milestoneView.listKeyboard(ml));
            }
            case "cancel" -> {
                conversationStore.clear(key);
                messageSender.edit(chatId, messageId, "✖ Cancelled.", null);
            }
            default -> throw new BotException("Unknown action.");
        }
    }

    // ===================== Текст =====================

    @Override
    public void handle(Message message, ConversationState state) {
        User actor = accessService.requireUser(message.getFrom().getId());
        ConversationKey key = new ConversationKey(message.getChatId(), message.getFrom().getId());
        if (!message.hasText()) {
            throw new BotException("Please send text.");
        }
        String text = message.getText().trim();
        Project p = projectService.getById(state.entityId());

        switch (Step.valueOf(state.step())) {
            case NAME -> {
                if (text.isEmpty() || text.length() > 200) {
                    throw new BotException("Milestone name must be 1–200 characters.");
                }
                state.data().put("name", text);
                conversationStore.advance(key, Step.DATE.name());
                messageSender.reply(message, milestoneView.datePrompt(p),
                        milestoneView.dateKeyboard(p, LocalDate.now(appProperties.timezone())));
            }
            case DATE -> {
                Milestone m = save(actor, state, DateParser.parse(text, "date"), key);
                MilestoneList ml = milestoneService.getMilestones(actor, m.getProject().getId());
                messageSender.reply(message,
                        "✅ Milestone added.\n\n" + milestoneView.list(ml), milestoneView.listKeyboard(ml));
            }
        }
    }

    // ===================== helpers =====================

    private Milestone save(User actor, ConversationState state, LocalDate date, ConversationKey key) {
        Milestone m = milestoneService.addMilestone(actor, state.entityId(), state.data().get("name"), date);
        conversationStore.clear(key);
        Project p = m.getProject();
        messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                "🎯 New milestone: " + m.getName() + " · " + m.getDueDate().format(DateParser.FORMAT)
                        + "\n(added by " + actor.getShortName() + ")");
        return m;
    }

    private ConversationState requireState(ConversationKey key, Step expected) {
        ConversationState state = conversationStore.get(key)
                .filter(s -> NAME.equals(s.handler()))
                .orElseThrow(() -> new BotException(
                        "This form has expired or belongs to someone else. Open Milestones again."));
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
