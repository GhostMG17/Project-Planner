package uz.mukhammad.projectplanner.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.mukhammad.projectplanner.bot.BotProperties;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TopicService;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserRepository;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2,10}$");
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TopicService topicService;
    private final MessageSender messageSender;
    private final BotProperties botProperties;

    public Project createProject(User creator, CreateProjectRequest req) {
        String code = req.code().trim().toUpperCase();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BotException("Project code must be 2–10 latin letters or digits, e.g. UTY.");
        }
        if (projectRepository.existsByCode(code)) {
            throw new BotException("Project with code " + code + " already exists.");
        }
        if (req.plannedEndDate().isBefore(req.startDate())) {
            throw new BotException("Planned end date can't be before start date.");
        }

        String pmUsername = req.pmUsername().trim().replace("@", "");
        User pm = userRepository.findByUsernameIgnoreCase(pmUsername)
                .orElseThrow(() -> new BotException(
                        "User @" + pmUsername + " not found. They must press /start in private chat first."));
        if (!pm.isActive() || pm.getRole() == Role.TEAM_MEMBER) {
            throw new BotException("@" + pmUsername + " can't be a Project Manager. "
                    + "Assign the role first: /setrole @" + pmUsername + " PROJECT_MANAGER");
        }

        Long chatId = botProperties.groupChatId();
        Integer threadId;
        try {
            threadId = topicService.createTopic(chatId, code + " · " + req.name().trim());
        } catch (TelegramApiException e) {
            log.error("Failed to create topic for project {}", code, e);
            throw new BotException("Could not create project topic. Check my permissions in the group.");
        }

        Project project = new Project();
        project.setCode(code);
        project.setName(req.name().trim());
        project.setClient(req.client().trim());
        project.setProjectManager(pm);
        project.setStartDate(req.startDate());
        project.setPlannedEndDate(req.plannedEndDate());
        project.setTelegramChatId(chatId);
        project.setTelegramThreadId(threadId);
        project.setCreatedBy(creator);

        try {
            project = projectRepository.save(project);
        } catch (RuntimeException e) {
            log.error("Failed to save project {}, removing topic {}", code, threadId, e);
            topicService.deleteTopicQuietly(chatId, threadId);
            throw e;
        }

        log.info("Project created: code={}, id={}, pm={}, by={}",
                code, project.getId(), pm.getTelegramId(), creator.getTelegramId());

        messageSender.send(chatId, threadId, formatCard(project));
        return project;
    }

    @Transactional(readOnly = true)
    public List<Project> findVisibleProjects(User user) {
        return switch (user.getRole()) {
            case ADMIN, PORTFOLIO_LEAD -> projectRepository.findAllByOrderByCodeAsc();
            case PROJECT_MANAGER -> projectRepository.findByProjectManagerOrderByCodeAsc(user);
            case TEAM_MEMBER -> List.of();
        };
    }

    private String formatCard(Project p) {
        User pm = p.getProjectManager();
        return "📁 " + p.getCode() + " · " + p.getName() + "\n\n"
                + "Client: " + p.getClient() + "\n"
                + "PM: " + pm.getDisplayName() + (pm.getUsername() != null ? " (@" + pm.getUsername() + ")" : "") + "\n"
                + "Period: " + p.getStartDate().format(DateParser.FORMAT)
                + " → " + p.getPlannedEndDate().format(DateParser.FORMAT) + "\n"
                + "Status: " + p.getStatus();
    }

}
