package test;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.fail;

public class TestClass {

    private static HashMap<String, List<String>> ITEMS = new HashMap<>();

    @BeforeAll
    static void loadDatabase() {
        System.out.println("Reading database...");
        String data = "";
        try (Scanner s = new Scanner(new File("db.json"))) {
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
    }

    @Test
    @DisplayName("Ricerca per Stringa")
    void testSearchString() {
        String search = "café-uniform dress";
        System.out.println("Searching for " + search);
        List<String> result = new ArrayList<>();
        for (String island : ITEMS.keySet()) {
            for(String item : ITEMS.get(island)) {
                if(item.equalsIgnoreCase(search)) {
                    System.out.println("Found " + item + " in Island " + island);
                    return;
                }
            }
        }
        fail();
    }
}
