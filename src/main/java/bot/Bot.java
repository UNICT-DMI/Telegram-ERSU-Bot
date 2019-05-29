package bot;

import command.HelpCommand;
import command.MenuCommand;
import command.ReportCommand;
import command.StartCommand;
import command.UfficioErsuCommand;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;


import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import parser.ParserMenu;

public class Bot extends TelegramLongPollingCommandBot {

    private final String PATH = YmlResolver.getInstance().getValue("path_mensa");
    private ParserMenu p = new ParserMenu(new File(PATH));
    private List<IBotCommand> commands;
    public Bot(String botUsername) {
        super(botUsername);
        super.register(new ReportCommand()); // 0
        super.register(new MenuCommand(p)); // 1
        super.register(new UfficioErsuCommand()); // 2
        super.register(new StartCommand()); // 3
        super.register(new HelpCommand()); // 4
        commands = super.getRegisteredCommands().stream().collect(Collectors.toList());

    }
    
    @Override
    public String getBotToken() {
        return YmlResolver.getInstance().getValue("token");
    }

    private void sendMessageToChannel(String message){ 
        SendMessage sd = new SendMessage();
        sd.enableMarkdown(true);
        sd.setChatId(YmlResolver.getInstance().getValue("mensa_channel"));

        sd.setText(message);
        sd.enableMarkdown(true);

        try {
            execute(sd);
        } catch (TelegramApiException ex) {
            Logger.getLogger(Bot.class).error("Errore invio messaggio al canale",ex);
        }
    }

    public void sendMenuToChannel() {
        sendMessageToChannel(p.getMenu());
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().isUserMessage()) {
            Message message = update.getMessage();
            onKeyboardCommands(update.getMessage());
            if (message.hasText()) {
                SendMessage echoMessage = new SendMessage()
                .setChatId(message.getChatId())
                .setText("Questo messaggio non mi comanda nulla: " + message.getText()
                + " \nScegli tra queste opzioni: ");

                try {
                    execute(echoMessage);
                } catch (TelegramApiException e) {
                    Logger.getLogger(Bot.class).error("Errore invio messaggio");
                }
            }
        }
    }
    
    protected void onKeyboardCommands(Message message)  {
        SendMessage sndMsg = new SendMessage().setChatId(message.getChatId());
        if (message.isUserMessage()) {
            ReplyKeyboardMarkup rkm = generateRKM();

            sndMsg.setReplyMarkup(rkm);
            sndMsg.setText("Seleziona un comando o digita /help");
            if (message.getText().equals("Ufficio ERSU Catania :books:")) 
                key(message, "ufficioersu");
            if (message.getText().equals("Menù mensa :fork_and_knife:")) 
                key(message,"menu");
            if (message.getText().equals("Help :question:")) 
                key(message,"help");
            if (message.getText().equals("Segnalazioni Rappresentanti :mailbox_with_mail:")) 
                sndMsg.setText("Usa il comando /report <inserisci qui la segnalazione>");
            
            try { 
                execute(sndMsg);
            } catch (TelegramApiException ex) {
                //Logger 
            }
        }
    }

    private void key(Message message, String identifier) {
        Optional<IBotCommand> ufficioErsuCommand =
                commands.stream().filter(x -> x.getCommandIdentifier().equals(identifier)).findFirst();
        if (ufficioErsuCommand.isPresent())
            ufficioErsuCommand.get().processMessage(this, message, new String[0]);
    }
   
    
    private ReplyKeyboardMarkup generateRKM() {
        ReplyKeyboardMarkup rkm = new ReplyKeyboardMarkup();
        List<KeyboardRow> commands = new ArrayList<KeyboardRow>(); 
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("Menù mensa :fork_and_knife:");
        firstRow.add("Ufficio ERSU Catania :books:");
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("Segnalazioni Rappresentanti :mailbox_with_mail:");
        secondRow.add("Help :question:"); 
        commands.add(firstRow);
        commands.add(secondRow);
        rkm.setResizeKeyboard(true);
        rkm.setOneTimeKeyboard(true);
        rkm.setKeyboard(commands);
        rkm.setSelective(true);
        return rkm; 
    }
    
}
