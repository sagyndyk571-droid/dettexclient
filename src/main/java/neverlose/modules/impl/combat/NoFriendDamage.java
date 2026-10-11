package neverlose.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.InteractEntityEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFriendDamage extends ModuleStructure {

    public NoFriendDamage() {
        super("NoFriendDamage", "No Friend Damage", ModuleCategory.COMBAT);
    }

    @EventHandler
    public void onAttack(InteractEntityEvent e) {
        e.setCancelled(FriendUtils.isFriend(e.getEntity()));
    }
}

