package neverlose.util.sounds;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import neverlose.IMinecraft;
import neverlose.util.string.PlayerInteractionHelper;

@Setter
@Getter
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SoundManager implements IMinecraft {
    public SoundEvent KOLOKOLNIA_KILL = SoundEvent.of(Identifier.of("neverlose:kolokolnia_kill"));
    public SoundEvent MOAN1 = SoundEvent.of(Identifier.of("neverlose:moan1"));
    public SoundEvent MOAN2 = SoundEvent.of(Identifier.of("neverlose:moan2"));
    public SoundEvent MOAN3 = SoundEvent.of(Identifier.of("neverlose:moan3"));
    public SoundEvent MOAN4 = SoundEvent.of(Identifier.of("neverlose:moan4"));
    public SoundEvent MODULE_DISABLE = SoundEvent.of(Identifier.of("neverlose:module_disable"));
    public SoundEvent MODULE_ENABLE = SoundEvent.of(Identifier.of("neverlose:module_enable"));
    public SoundEvent OFF = SoundEvent.of(Identifier.of("neverlose:off"));
    public SoundEvent ON = SoundEvent.of(Identifier.of("neverlose:on"));
    public SoundEvent CRIME = SoundEvent.of(Identifier.of("neverlose:crime"));
    public SoundEvent METALLIC = SoundEvent.of(Identifier.of("neverlose:metallic"));
    public SoundEvent WELCOME = SoundEvent.of(Identifier.of("neverlose:welcome"));
    public SoundEvent CLICKGUI_CLACK = SoundEvent.of(Identifier.of("neverlose:clickgui_clack"));
    public SoundEvent CLICKGUI_FUNCTION_OFF = SoundEvent.of(Identifier.of("neverlose:clickgui_function_off"));
    public SoundEvent CLICKGUI_FUNCTION_ON = SoundEvent.of(Identifier.of("neverlose:clickgui_function_on"));
    public SoundEvent CLICKGUI_CLOSE = SoundEvent.of(Identifier.of("neverlose:clickgui_close"));
    public SoundEvent CLICKGUI_OPEN = SoundEvent.of(Identifier.of("neverlose:clickgui_open"));
    public SoundEvent CLICKGUI_MODULE_CLOSE = SoundEvent.of(Identifier.of("neverlose:clickgui_module_close"));
    public SoundEvent CLICKGUI_MODULE_OPEN = SoundEvent.of(Identifier.of("neverlose:clickgui_module_open"));
    public SoundEvent CLICKGUI_SELECT = SoundEvent.of(Identifier.of("neverlose:clickgui_select"));
    public SoundEvent CLICKGUI_SLIDER = SoundEvent.of(Identifier.of("neverlose:clickgui_slider"));
    public SoundEvent CLICKGUI_TITLE = SoundEvent.of(Identifier.of("neverlose:clickgui_title"));

    public void init() {
        Registry.register(Registries.SOUND_EVENT, KOLOKOLNIA_KILL.id(), KOLOKOLNIA_KILL);
        Registry.register(Registries.SOUND_EVENT, MOAN1.id(), MOAN1);
        Registry.register(Registries.SOUND_EVENT, MOAN2.id(), MOAN2);
        Registry.register(Registries.SOUND_EVENT, MOAN3.id(), MOAN3);
        Registry.register(Registries.SOUND_EVENT, MOAN4.id(), MOAN4);
        Registry.register(Registries.SOUND_EVENT, MODULE_DISABLE.id(), MODULE_DISABLE);
        Registry.register(Registries.SOUND_EVENT, MODULE_ENABLE.id(), MODULE_ENABLE);
        Registry.register(Registries.SOUND_EVENT, OFF.id(), OFF);
        Registry.register(Registries.SOUND_EVENT, ON.id(), ON);
        Registry.register(Registries.SOUND_EVENT, CRIME.id(), CRIME);
        Registry.register(Registries.SOUND_EVENT, METALLIC.id(), METALLIC);
        Registry.register(Registries.SOUND_EVENT, WELCOME.id(), WELCOME);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_CLACK.id(), CLICKGUI_CLACK);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_FUNCTION_OFF.id(), CLICKGUI_FUNCTION_OFF);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_FUNCTION_ON.id(), CLICKGUI_FUNCTION_ON);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_CLOSE.id(), CLICKGUI_CLOSE);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_OPEN.id(), CLICKGUI_OPEN);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_MODULE_CLOSE.id(), CLICKGUI_MODULE_CLOSE);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_MODULE_OPEN.id(), CLICKGUI_MODULE_OPEN);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_SELECT.id(), CLICKGUI_SELECT);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_SLIDER.id(), CLICKGUI_SLIDER);
        Registry.register(Registries.SOUND_EVENT, CLICKGUI_TITLE.id(), CLICKGUI_TITLE);
    }

    public void playSound(SoundEvent sound) {
        playSound(sound, 1, 1);
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!PlayerInteractionHelper.nullCheck()) {
            mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
        }
    }

    public void playSoundDirect(SoundEvent sound, float volume, float pitch) {
        mc.getSoundManager().play(PositionedSoundInstance.ui(sound, pitch, volume));
    }
}
