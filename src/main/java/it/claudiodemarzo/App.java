package it.claudiodemarzo;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

public class App {
    private static final String TOKEN = "dmwc3wx56hlhx73lkoh7oqlorkd49x";
    private static final String CHANNEL_NAME = "treasureislands";
    private static final String VERSION = "1.4";
    private static String UNAVAILABLE_STRING;

    private static TwitchClient client;
    private static final File DATABASE = new File("db.json"),
            CONFIG = new File("config.json");
    private static final HashMap<String, ArrayList<String>> ITEMS = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("treasurecatalogue v" + VERSION + " starting...");
        System.out.println("Reading database...");
        String data = "";
        try (Scanner s = new Scanner(DATABASE)) {
            while (s.hasNextLine()) {
                data += s.nextLine();
            }
        } catch (Exception e) {
            System.err.println("Error while reading database");
            System.exit(-1);
        }
        System.out.println("Database read successfully");
        System.out.println("Parsing database...");
        try {
            JSONObject dbObj = new JSONObject(data).getJSONObject("data");
            for (String key : dbObj.keySet()) {
                JSONArray arr = dbObj.getJSONArray(key);
                for (int i = 0; i < arr.length(); i++) {
                    System.out.println("Processed Item " + arr.getString(i) + " in Island " + key);
                    ITEMS.computeIfAbsent(key, k -> new ArrayList<>());
                    ITEMS.get(key).add(arr.getString(i).toLowerCase());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error while parsing database");
            System.exit(-1);
        }
        System.out.println("Database parsed successfully");

        System.out.println("Reading config...");
        data = "";
        try (Scanner s = new Scanner(CONFIG)) {
            while (s.hasNextLine()) {
                data += s.nextLine();
            }
        } catch (Exception e) {
            System.err.println("Error while reading config");
            System.exit(-1);
        }
        System.out.println("Config read successfully");
        System.out.println("Parsing config...");
        try {
            JSONObject configObj = new JSONObject(data);
            UNAVAILABLE_STRING = configObj.getString("unavailable_string");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error while parsing config");
            System.exit(-1);
        }
        System.out.println("Config parsed successfully");

        System.out.println("Connecting to Twitch...");
        client = TwitchClientBuilder.builder()
                .withChatAccount(new OAuth2Credential("twitch", TOKEN))
                .withDefaultAuthToken(new OAuth2Credential("twitch", TOKEN))
                .withEnableChat(true)
                .build();
        System.out.println("Connection to Twitch successful!");

        System.out.println("Joining channel " + CHANNEL_NAME + "...");
        client.getChat().joinChannel(CHANNEL_NAME);
        System.out.println("Joined channel " + CHANNEL_NAME + " successfully!");

        System.out.println("Listening for messages...");
        client.getEventManager().onEvent(ChannelMessageEvent.class, cme -> {
            String message = cme.getMessage();
            String[] arguments = null;
            boolean hasArgs = false;
            if (message.startsWith("!")) {        //is a command
                /*System.out.println("Command recognized");*/
                StringTokenizer st = new StringTokenizer(message.substring(1), " ");

                if (st.countTokens() > 1) {     //args check
                    hasArgs = true;
                    arguments = new String[st.countTokens() - 1];
                }

                String command = st.nextToken(); //separating args from command
                if (hasArgs) {
                    int i = 0;
                    while (st.hasMoreTokens()) {
                        arguments[i] = st.nextToken();
                        i++;
                    }
                }
                if (hasArgs) {
                    System.err.println("Command '" + command + "' recognized. Printing arguments");
                    Arrays.stream(arguments).forEach(System.err::println);
                }
                switch (command.toLowerCase()) {
                    case "search":
                    case "find":
                    case "locate":
                        if (!hasArgs) {
                            client.getChat().sendMessage(CHANNEL_NAME, "Usage: !search/find/locate <item>");
                            break;
                        }
                        String query = String.join(" ", arguments).trim();
                        System.err.println("Querying item: " + query);
                        List<String> result = search(query);
                        if (result.isEmpty()) {
                            System.err.println("Item " + query + " not found");
                            client.getChat().sendMessage(CHANNEL_NAME, UNAVAILABLE_STRING);
                        } else {
                            if(result.size() == 1) {
                                System.err.println("Item " + query + " found on " + result.get(0));
                                client.getChat().sendMessage(CHANNEL_NAME, "Item " + query + " found on " + result.get(0));
                            }
                            else {
                                System.err.println("Item " + query + " found on the following islands: " + String.join(", ", result));
                                client.getChat().sendMessage(CHANNEL_NAME, "Item " + query + " found on the following islands: " + String.join(", ", result));
                            }
                        }
                        break;
                    case "reload":
                        if (!cme.getUser().getName().equalsIgnoreCase(CHANNEL_NAME)) break;
                        String data_ = "";
                        System.out.println("Reloading database...");
                        try (Scanner s = new Scanner(DATABASE)) {
                            while (s.hasNextLine()) {
                                data_ += s.nextLine();
                            }
                        } catch (Exception e) {
                            System.err.println("Error while reading database");
                            System.exit(-1);
                        }
                        System.out.println("Database read successfully");
                        System.out.println("Clearing in-memory database...");
                        for (String key : ITEMS.keySet())
                            ITEMS.get(key).clear();
                        System.out.println("In-memory database cleared successfully");
                        System.out.println("Parsing database...");
                        try {
                            JSONObject dbObj = new JSONObject(data_).getJSONObject("data");
                            for (String key : dbObj.keySet()) {
                                JSONArray arr = dbObj.getJSONArray(key);
                                for (int i = 0; i < arr.length(); i++) {
                                    System.out.println("Processed Item " + arr.getString(i) + " in Island " + key);
                                    ITEMS.computeIfAbsent(key, k -> new ArrayList<>());
                                    ITEMS.get(key).add(arr.getString(i).toLowerCase());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Error while parsing database");
                            System.exit(-1);
                        }
                        System.out.println("Database parsed successfully");
                        break;
                }
            }
        });
    }

    private static List<String> search(String query) {
        List<String> result = new ArrayList<>();
        for (String island : ITEMS.keySet()) {
            for(String item : ITEMS.get(island)) {
                if(item.toLowerCase().contains(query.toLowerCase())) {
                    result.add(island);
                }
            }
        }
        return result;
    }
}