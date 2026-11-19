package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TopicService;
import uz.mukhammad.projectplanner.bot.conversation.ConversationKey;
import uz.mukhammad.projectplanner.bot.conversation.ConversationStore;
import uz.mukhammad.projectplanner.bot.conversation.ProjectEditInputHandler;
import uz.mukhammad.projectplanner.bot.view.ProjectView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.notification.service.ProjectRiskChecker;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectField;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.record.ProjectTeam;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class ProjectCallbackHandler implements CallbackHandler {

    private final AccessService accessService;
    private final ProjectService projectService;
    private final ProjectView projectView;
    private final MessageSender messageSender;
    private final TopicService topicService;
    private final ConversationStore conversationStore;
    private final ProjectRiskChecker projectRiskChecker;

    @Override
    public String prefix() {
        return ProjectView.PREFIX;
    }

    @Override
    public void handle(CallbackQuery cq) {
        String[] parts = cq.getData().split(":");
        if (parts.length != 3) {
            throw new BotException("Unknown action.");
        }
        String action = parts[1];
        Long projectId = parseId(parts[2]);

        User actor = accessService.requireUser(cq.getFrom().getId());
        Long chatId = cq.getMessage().getChatId();
        Integer messageId = cq.getMessage().getMessageId();

        switch (action) {
            case "card" -> {
                conversationStore.clear(new ConversationKey(chatId, cq.getFrom().getId()));
                Project p = projectService.getById(projectId);
                projectService.requireCanView(actor, p);
                messageSender.edit(chatId, messageId, projectView.card(p),
                        projectView.cardKeyboard(p, projectService.canManage(actor, p)));
            }
            case "team" -> {
                Project p = projectService.getById(projectId);
                ProjectTeam team = projectService.getTeam(actor, p.getCode());
                messageSender.edit(chatId, messageId, projectView.team(team), projectView.backKeyboard(p));
            }
            case "close" -> {
                Project p = projectService.getById(projectId);
                projectService.requireCanManage(actor, p);
                messageSender.edit(chatId, messageId,
                        "Close project " + p.getCode() + "?\nIt will be marked as COMPLETED.",
                        projectView.confirmKeyboard(p, "close_ok", "✅ Yes, close"));
            }
            case "close_ok" -> {
                Project p = projectService.closeProject(actor, projectId);
                messageSender.edit(chatId, messageId, projectView.card(p),
                        projectView.cardKeyboard(p, true));
                messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                        "✅ Project marked as COMPLETED by " + actor.getDisplayName() + ".");
            }
            case "archive" -> {
                Project p = projectService.getById(projectId);
                projectService.requireCanManage(actor, p);
                messageSender.edit(chatId, messageId,
                        "Archive project " + p.getCode() + "?\nIts topic will be closed.",
                        projectView.confirmKeyboard(p, "archive_ok", "🗄 Yes, archive"));
            }
            case "archive_ok" -> {
                Project p = projectService.archiveProject(actor, projectId);
                messageSender.edit(chatId, messageId, projectView.card(p),
                        projectView.cardKeyboard(p, true));
                messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                        "🗄 Project archived by " + actor.getDisplayName() + ". This topic is now closed.");
                topicService.closeTopicQuietly(p.getTelegramChatId(), p.getTelegramThreadId());
            }
            case "edit" -> {
                Project p = projectService.getById(projectId);
                projectService.requireCanManage(actor, p);
                messageSender.edit(chatId, messageId,
                        "✏️ What do you want to edit in " + p.getCode() + "?",
                        projectView.editMenuKeyboard(p));
            }
            case "risk" -> {
                Project p = projectService.toggleRiskFlag(actor, projectId);
                messageSender.edit(chatId, messageId, projectView.card(p), projectView.cardKeyboard(p, true));
                messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(), p.isAtRiskManual()
                        ? "⚠️ Project flagged AT RISK by " + actor.getShortName() + "."
                        : "✅ Risk flag cleared by " + actor.getShortName() + ".");
                projectRiskChecker.check(p);
            }
            default -> {
                if (action.startsWith("edit_")) {
                    startEdit(cq, actor, projectId, action.substring("edit_".length()), chatId, messageId);
                } else {
                    throw new BotException("Unknown action.");
                }
            }
        }
    }


    private void startEdit(CallbackQuery cq, User actor, Long projectId, String fieldKey,
                           Long chatId, Integer messageId) {
        ProjectField field = ProjectField.fromKey(fieldKey);
        Project p = projectService.getById(projectId);
        projectService.requireCanManage(actor, p);
        if (p.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BotException("Archived project can't be edited.");
        }

        conversationStore.start(new ConversationKey(chatId, cq.getFrom().getId()),
                ProjectEditInputHandler.NAME, field.name(), projectId);

        messageSender.edit(chatId, messageId, projectView.editPrompt(p, field), projectView.cancelKeyboard(p));
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new BotException("Unknown action.");
        }
    }
}
