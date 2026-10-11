package neverlose.cosmetics;

public enum CosmeticType {
    MODEL("Model", "Models"),
    PET("Pet", "Pets"),
    ACCESSORY("Accessory", "Accessories");

    private final String id;
    private final String displayName;

    CosmeticType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CosmeticType fromJson(String raw) {
        if (raw == null || raw.isBlank()) return MODEL;
        return switch (raw.toLowerCase()) {
            case "pet", "pets" -> PET;
            case "accessory", "accessories", "item", "items" -> ACCESSORY;
            default -> MODEL;
        };
    }
}

