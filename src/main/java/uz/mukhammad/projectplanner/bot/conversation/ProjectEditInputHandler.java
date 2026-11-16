package uz.mukhammad.projectplanner.bot.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TopicService;
import uz.mukhammad.projectplanner.bot.view.ProjectView;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.project.ProjectField;
import uz.mukhammad.projectplanner.project.ProjectService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class ProjectEditInputHandler implements InputHandler {

    public static final String NAME = "project_edit";

    private final AccessService accessService;
    private final ProjectService projectService;
    private final ProjectView projectView;
    private final MessageSender messageSender;
    private final ConversationStore conversationStore;
    private final TopicService topicService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void handle(Message message, ConversationState state) {
        User actor = accessService.requireUser(message.getFrom().getId());
        ProjectField field = ProjectField.valueOf(state.step());

        Project p = projectService.updateField(actor, state.entityId(), field, message.getText());

        conversationStore.clear(new ConversationKey(message.getChatId(), message.getFrom().getId()));

        if (field == ProjectField.NAME) {
            topicService.renameTopicQuietly(p.getTelegramChatId(), p.getTelegramThreadId(),
                    p.getCode() + " · " + p.getName());
        }

        messageSender.reply(message, "✅ " + field.getLabel() + " updated.\n\n" + projectView.card(p),
                projectView.cardKeyboard(p, true));

        boolean editedInProjectTopic = message.getChatId().equals(p.getTelegramChatId())
                && p.getTelegramThreadId().equals(message.getMessageThreadId());
        if (!editedInProjectTopic) {
            messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                    "✏️ " + field.getLabel() + " updated by " + actor.getDisplayName() + ".");
        }
    }
}
