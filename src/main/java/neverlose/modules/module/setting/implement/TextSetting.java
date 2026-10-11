package neverlose.modules.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import neverlose.modules.module.setting.Setting;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class TextSetting extends Setting {
    private String text;
    private int min, max;

    public TextSetting(String name, String description) {
        super(name, description);
    }

    public TextSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public String getValue() {
        return text;
    }

    public void setValue(String value) {
        this.text = value;
    }
}