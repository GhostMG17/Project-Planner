package uz.mukhammad.projectplanner.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.mukhammad.projectplanner.bot.BotProperties;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TopicService;
import uz.mukhammad.projectplanner.bot.view.ProjectView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.project_member.ProjectMember;
import uz.mukhammad.projectplanner.project_member.ProjectMemberRepository;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserRepository;
import uz.mukhammad.projectplanner.user.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2,10}$");
    private final ProjectRepository projectRepository;
    private final ProjectView projectView;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UserService userService;
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

        messageSender.send(chatId, threadId, projectView.card(project), projectView.cardKeyboard(project, true));
        return project;
    }

    @Transactional(readOnly = true)
    public List<Project> findVisibleProjects(User user) {
        return switch (user.getRole()) {
            case ADMIN, PORTFOLIO_LEAD -> projectRepository.findAllByOrderByCodeAsc();
            case PROJECT_MANAGER, TEAM_MEMBER -> projectRepository.findVisibleFor(user);
        };
    }

    @Transactional(readOnly = true)
    public Project getByCode(String rawCode) {
        String code = rawCode.trim().toUpperCase();
        return projectRepository.findByCode(code)
                .orElseThrow(() -> new BotException(
                        "Project " + code + " not found. Use /projects to see available projects."));
    }

    public boolean canManage(User actor, Project project) {
        return actor.getRole() == Role.ADMIN
                || actor.getRole() == Role.PORTFOLIO_LEAD
                || project.getProjectManager().getId().equals(actor.getId());
    }

    public void requireCanManage(User actor, Project project) {
        if (!canManage(actor, project)) {
            throw new BotException("Only the PM of " + project.getCode() + " or Portfolio Lead can do this.");
        }
    }

    @Transactional(readOnly = true)
    public Project getById(Long id) {
        return projectRepository.findWithPmById(id)
                .orElseThrow(() -> new BotException("Project not found."));
    }

    @Transactional
    public Project closeProject(User actor, Long projectId) {
        Project project = getById(projectId);
        requireCanManage(actor, project);
        if (project.getStatus() != ProjectStatus.ACTIVE && project.getStatus() != ProjectStatus.ON_HOLD) {
            throw new BotException("Project " + project.getCode() + " is already " + project.getStatus() + ".");
        }
        project.setStatus(ProjectStatus.COMPLETED);
        log.info("Project closed: {} by {}", project.getCode(), actor.getTelegramId());
        return project;
    }

    @Transactional
    public Project archiveProject(User actor, Long projectId) {
        Project project = getById(projectId);
        requireCanManage(actor, project);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BotException("Project " + project.getCode() + " is already archived.");
        }
        project.setStatus(ProjectStatus.ARCHIVED);
        log.info("Project archived: {} by {}", project.getCode(), actor.getTelegramId());
        return project;
    }

    public void requireCanView(User actor, Project project) {
        boolean allowed = actor.getRole() == Role.ADMIN
                || actor.getRole() == Role.PORTFOLIO_LEAD
                || project.getProjectManager().getId().equals(actor.getId())
                || memberRepository.existsByProjectAndUser(project, actor);
        if (!allowed) {
            throw new BotException("You don't have access to project " + project.getCode() + ".");
        }
    }

    @Transactional
    public MemberChange addMember(User actor, String projectCode, String username) {
        Project project = getByCode(projectCode);
        requireCanManage(actor, project);
        User user = userService.getByUsername(username);

        if (project.getProjectManager().getId().equals(user.getId())) {
            throw new BotException("@" + user.getUsername() + " is already the PM of " + project.getCode() + ".");
        }
        if (memberRepository.existsByProjectAndUser(project, user)) {
            throw new BotException("@" + user.getUsername() + " is already in the " + project.getCode() + " team.");
        }

        memberRepository.save(new ProjectMember(project, user, actor));
        log.info("Member added: project={}, user={}, by={}",
                project.getCode(), user.getTelegramId(), actor.getTelegramId());
        return new MemberChange(project, user);
    }

    @Transactional
    public MemberChange removeMember(User actor, String projectCode, String username) {
        Project project = getByCode(projectCode);
        requireCanManage(actor, project);
        User user = userService.getByUsername(username);

        ProjectMember member = memberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new BotException(
                        "@" + user.getUsername() + " is not in the " + project.getCode() + " team."));

        memberRepository.delete(member);
        log.info("Member removed: project={}, user={}, by={}",
                project.getCode(), user.getTelegramId(), actor.getTelegramId());
        return new MemberChange(project, user);
    }

    @Transactional
    public MemberChange changeProjectManager(User actor, String projectCode, String username) {
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.PORTFOLIO_LEAD) {
            throw new BotException("Only Portfolio Lead can change the project manager.");
        }
        Project project = getByCode(projectCode);
        User newPm = userService.getByUsername(username);

        if (newPm.getRole() == Role.TEAM_MEMBER) {
            throw new BotException("@" + newPm.getUsername() + " can't be a Project Manager. "
                    + "Assign the role first: /setrole @" + newPm.getUsername() + " PROJECT_MANAGER");
        }
        User oldPm = project.getProjectManager();
        if (oldPm.getId().equals(newPm.getId())) {
            throw new BotException("@" + newPm.getUsername() + " is already the PM of " + project.getCode() + ".");
        }

        memberRepository.findByProjectAndUser(project, newPm).ifPresent(memberRepository::delete);
        if (!memberRepository.existsByProjectAndUser(project, oldPm)) {
            memberRepository.save(new ProjectMember(project, oldPm, actor));
        }

        project.setProjectManager(newPm);
        log.info("PM changed: project={}, {} -> {}, by={}",
                project.getCode(), oldPm.getTelegramId(), newPm.getTelegramId(), actor.getTelegramId());
        return new MemberChange(project, newPm);
    }

    @Transactional(readOnly = true)
    public ProjectTeam getTeam(User actor, String projectCode) {
        Project project = getByCode(projectCode);
        requireCanView(actor, project);
        List<User> members = memberRepository.findByProjectOrderByAddedAtAsc(project).stream()
                .map(ProjectMember::getUser)
                .toList();
        return new ProjectTeam(project, members);
    }


    @Transactional
    public Project updateField(User actor, Long projectId, ProjectField field, String rawValue) {
        Project p = getById(projectId);
        requireCanManage(actor, p);
        if (p.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BotException("Archived project can't be edited.");
        }

        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            throw new BotException("Value can't be empty.");
        }

        switch (field) {
            case NAME -> p.setName(requireMaxLength(value, 200, "Name"));
            case CLIENT -> p.setClient(requireMaxLength(value, 200, "Client"));
            case DESCRIPTION -> p.setDescription(requireMaxLength(value, 2000, "Description"));
            case DELIVERABLES -> p.setKeyDeliverables(requireMaxLength(value, 2000, "Key deliverables"));
            case START_DATE -> {
                LocalDate start = DateParser.parse(value, "start date");
                if (start.isAfter(p.getPlannedEndDate())) {
                    throw new BotException("Start date can't be after end date ("
                            + p.getPlannedEndDate().format(DateParser.FORMAT) + ").");
                }
                p.setStartDate(start);
            }
            case END_DATE -> {
                LocalDate end = DateParser.parse(value, "end date");
                if (end.isBefore(p.getStartDate())) {
                    throw new BotException("End date can't be before start date ("
                            + p.getStartDate().format(DateParser.FORMAT) + ").");
                }
                p.setPlannedEndDate(end);
            }
        }

        log.info("Project {} field {} updated by {}", p.getCode(), field, actor.getTelegramId());
        return p;
    }

    private String requireMaxLength(String value, int max, String fieldName) {
        if (value.length() > max) {
            throw new BotException(fieldName + " is too long (max " + max + " characters).");
        }
        return value;
    }



}
