package com.chaotic_loom.game.core.util;

import java.util.HashMap;
import java.util.Map;

public class ArgsManager {
    private final Map<String, String> argsMap = new HashMap<>();

    // Constructor that parses the command-line arguments
    public ArgsManager(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String current = args[i];
            if (current.startsWith("-")) {
                // Remove leading dashes (- or --)
                String key = current.replaceFirst("^-+", "");
                String value = "true"; // Default value for flags without a dedicated value

                // If the next argument exists and does not start with a dash, it's the value
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    value = args[i + 1];
                    i++; // Skip the next element as it has been consumed as a value
                }
                argsMap.put(key, value);
            } else {
                // Optionally: handle positional arguments here if needed.
            }
        }
    }

    public boolean has(String argName) {
        return argsMap.containsKey(argName);
    }

    public String getValue(String argName) {
        return argsMap.get(argName);
    }

    // Throws an exception if the specified argument is missing.
    public void throwIfMissing(String argName, String crashReason) {
        if (!has(argName)) {
            String message = "Missing argument: '" + argName + "'";
            if (crashReason != null && !crashReason.isEmpty()) {
                message += " - " + crashReason;
            }
            throw new IllegalArgumentException(message);
        }
    }

    public void throwIfMissing(String argName) {
        throwIfMissing(argName, null);
    }
}
