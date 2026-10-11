package neverlose.modules.impl.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.api.events.render.TextFactoryEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.modules.esp.RwPrefix;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DonateFix extends ModuleStructure {

    SelectSetting names = new SelectSetting("Названия", "Как показывать донатные glyph-префиксы")
            .value("Русские", "English");

    BooleanSetting colors = new BooleanSetting("Цвета", "Красить донаты как на ReallyWorld")
            .setValue(true);

    BooleanSetting brackets = new BooleanSetting("Скобки", "Показывать донат в квадратных скобках")
            .setValue(true);

    public DonateFix() {
        super("DonateFix", "Shows ReallyWorld donate prefixes without server resource-pack", ModuleCategory.MISC);
        settings(names, colors, brackets);
    }

    @EventHandler
    public void onTextFactory(TextFactoryEvent event) {
        String text = event.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        String fixed = replaceDonateGlyphs(text);
        if (!fixed.equals(text)) {
            event.setText(fixed);
        }
    }

    private String replaceDonateGlyphs(String text) {
        StringBuilder result = new StringBuilder(text.length() + 24);
        boolean changed = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String label = RwPrefix.getIconLabel(c);

            if (label == null) {
                result.append(c);
                continue;
            }

            changed = true;
            appendSpaceBeforeRank(result);
            result.append(formatRank(label));

            int skipped = 0;
            while (i + 1 < text.length() && skipped < 3) {
                char next = text.charAt(i + 1);
                if (!RwPrefix.isIcon(next) || RwPrefix.getIconLabel(next) != null) {
                    break;
                }
                i++;
                skipped++;
            }

            if (i + 1 < text.length() && needsSpaceAfterRank(text.charAt(i + 1))) {
                result.append(' ');
            }
        }

        return changed ? result.toString() : text;
    }

    private void appendSpaceBeforeRank(StringBuilder result) {
        if (result.isEmpty()) {
            return;
        }

        char previous = result.charAt(result.length() - 1);
        if (!Character.isWhitespace(previous) && previous != '[' && previous != '(') {
            result.append(' ');
        }
    }

    private boolean needsSpaceAfterRank(char next) {
        return !Character.isWhitespace(next)
                && next != ']'
                && next != ')'
                && next != ','
                && next != '.'
                && next != ':'
                && next != ';';
    }

    private String formatRank(String label) {
        String title = names.isSelected("Русские") ? getRussianLabel(label) : label;
        String text = brackets.isValue() ? "[" + title + "]" : title;

        if (!colors.isValue()) {
            return text;
        }

        return getColorCode(label) + text + "\u00A7r";
    }

    private String getRussianLabel(String label) {
        return switch (label) {
            case "IMPERATOR" -> "Император";
            case "TITAN" -> "Титан";
            case "PLAYER" -> "Игрок";
            case "DRAGON" -> "Драгон";
            case "MODER" -> "Модер";
            case "ADMIN" -> "Админ";
            case "OVERLORD" -> "Оверлорд";
            case "MAGISTER" -> "Магистр";
            case "HERO" -> "HERO";
            case "AVENGER" -> "Avenger";
            case "BULL" -> "Бык";
            case "RABBIT" -> "Кролик";
            case "TIGER" -> "Тигр";
            case "DRACULA" -> "Дракула";
            case "BUNNY" -> "Bunny";
            case "HYDRA" -> "Гидра";
            case "COBRA" -> "Кобра";
            case "MEDIA" -> "Media";
            case "YT" -> "YT";
            case "D.HELPER" -> "D.Helper";
            case "HELPER" -> "Хелпер";
            case "ML.MODER" -> "Мл.Модер";
            case "MODER+" -> "Модер+";
            case "ST.MODER" -> "Ст.Модер";
            case "GL.MODER" -> "Гл.Модер";
            case "ML.ADMIN" -> "Мл.Админ";
            case "VAMPIRE" -> "Вампир";
            case "PEGAS" -> "Пегас";
            default -> label;
        };
    }

    private String getColorCode(String label) {
        return switch (label) {
            case "IMPERATOR", "ADMIN", "ML.ADMIN" -> "\u00A7c";
            case "TITAN" -> "\u00A7e";
            case "PLAYER" -> "\u00A77";
            case "DRAGON" -> "\u00A7d";
            case "MODER", "MODER+", "ML.MODER", "ST.MODER", "GL.MODER", "OVERLORD", "AVENGER" -> "\u00A7b";
            case "MAGISTER" -> "\u00A76";
            case "HERO" -> "\u00A79";
            case "HELPER", "D.HELPER", "MEDIA", "YT" -> "\u00A7a";
            default -> "\u00A7f";
        };
    }
}
