package neverlose.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.api.events.render.TextFactoryEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.TextSetting;
import neverlose.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NameProtect extends ModuleStructure {

    TextSetting nameSetting = new TextSetting("Имя", "Никнейм, который будет заменен на ваш").setText("Protected").setMax(32);
    BooleanSetting friendsSetting = new BooleanSetting("Друзья", "Скрывает никнеймы друзей").setValue(true);

    public NameProtect() {
        super("NameProtect","Name Protect", ModuleCategory.PLAYER);
        settings(friendsSetting);
    }

    @EventHandler
    public void onTextFactory(TextFactoryEvent e) {
        e.replaceText(mc.getSession().getUsername(), nameSetting.getText());
        if (friendsSetting.isValue()) {
            replaceFriendNames(e);
        }
    }
    private void replaceFriendNames(TextFactoryEvent e) {
        FriendUtils.getFriends().forEach(friend -> e.replaceText(friend.getName(), nameSetting.getText()));
    }
}