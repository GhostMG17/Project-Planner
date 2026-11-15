package uz.mukhammad.projectplanner.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.user.RegistrationResult;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

@Component
@RequiredArgsConstructor
public class StartCommand implements CommandHandler {

    private final UserService userService;
    private final MessageSender messageSender;

    @Override public String command() { return "/start"; }
    @Override public String description() { return "Register and start using the bot"; }


    @Override
    public void handle(Message message) {
        if (!message.getChat().isUserChat()){
            messageSender.reply(message,
                    "Please open a private chat with me and press /start to register.");
            return;
        }

        var from = message.getFrom();
        RegistrationResult result = userService.registerOrUpdate(
                from.getId(), from.getUserName(), from.getFirstName(), from.getLastName());

        User user = result.user();
        String text = result.created()
                ? "Welcome, " + user.getFirstName() + "! You're registered as " + user.getRole() +"."
                : "Welcome back, " + user.getFirstName() + "! Your role: " + user.getRole() + ".";
        messageSender.reply(message,text);
    }

}
