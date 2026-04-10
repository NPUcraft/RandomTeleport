package com.npucraft.randomteleport.config;

public enum LanguageMode {
    AUTO,
    ZH,
    EN;

    public static LanguageMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        return switch (raw.trim().toLowerCase()) {
            case "zh", "zh_cn", "chinese", "中文" -> ZH;
            case "en", "en_us", "english" -> EN;
            default -> AUTO;
        };
    }
}
