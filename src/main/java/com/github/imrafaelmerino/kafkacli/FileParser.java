package com.github.imrafaelmerino.kafkacli;

import jsonvalues.JsObj;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileParser {


    private static final Pattern KEY_PATTERN = Pattern.compile("^key: (.+)$",
                                                               Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_PATTERN = Pattern.compile("^value: (.+)$",
                                                                 Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADERS_PATTERN = Pattern.compile("^headers: (.+)$",
                                                                   Pattern.CASE_INSENSITIVE);

    public static List<Message> parseRecordsFromFile(String filePath) {
        List<Message> records = new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String key = "";
        String value = "";
        JsObj headers = JsObj.empty();

        for (String line : lines) {
            Matcher keyMatcher = KEY_PATTERN.matcher(line);
            Matcher valueMatcher = VALUE_PATTERN.matcher(line);
            Matcher headersMatcher = HEADERS_PATTERN.matcher(line);

            if (keyMatcher.matches()) {
                key = keyMatcher.group(1)
                                .trim();
            } else if (valueMatcher.matches()) {
                value = valueMatcher.group(1)
                                    .trim();
            } else if (headersMatcher.matches()) {
                headers = JsObj.parse(headersMatcher.group(1)
                                                    .trim());
            } else if (!line.isBlank()) {
                records.add(new Message(key,
                                        value,
                                        headers));
                key = "";
                value = "";
                headers = JsObj.empty();
            }
        }

        if (!value.isBlank()) {
            records.add(new Message(key,
                                    value,
                                    headers));
        }

        return records;
    }

}
