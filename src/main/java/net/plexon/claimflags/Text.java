package net.plexon.claimflags;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY = Pattern.compile("&([0-9A-FK-ORa-fk-or])");
    private Text() {}
    public static String color(String input) {
        if (input == null) return "";
        Matcher matcher = HEX.matcher(input); StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1); StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(out);
        return LEGACY.matcher(out.toString()).replaceAll("§$1");
    }
    public static String placeholders(String input, Map<String, String> values) {
        String result = input == null ? "" : input;
        for (Map.Entry<String, String> entry : values.entrySet()) result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        return result;
    }
    public static void send(FileConfiguration config, CommandSender sender, String path, Map<String, String> values) {
        String prefix = config.getString("messages.prefix", ""); Map<String, String> merged = new LinkedHashMap<>(values); merged.put("prefix", prefix);
        sender.sendMessage(color(placeholders(config.getString(path, ""), merged)));
    }
}
