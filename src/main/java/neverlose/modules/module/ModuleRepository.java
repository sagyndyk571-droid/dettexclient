package neverlose.modules.module;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.modules.impl.combat.*;
import neverlose.modules.impl.combat.NoInteract;
import neverlose.modules.impl.combat.AutoTotem;
import neverlose.modules.impl.misc.*;
import neverlose.modules.impl.misc.autoparser.AutoParser;
import neverlose.modules.impl.movement.*;
import neverlose.modules.impl.player.*;
import neverlose.modules.impl.render.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<ModuleStructure> moduleStructures = new ArrayList<>();
    List<ModuleStructure> hiddenModules = new ArrayList<>();
    Set<Class<? extends ModuleStructure>> registeredClasses = new HashSet<>();

    public void setup() {
        builder()
                .add(new Hud())
                .add(new CrossHair())
                .add(new Aura())
                .add(new Esp())
                .add(new SkeletonESP())
                .add(new AutoTool())
                .add(new ProjectileTracer())
                .add(new FpsBoost())
                .add(new FireWorkESP())
                .add(new RichDog())
                .add(new Trails())
                .add(new RegionExploit())
                .add(new WorldParticles())
                .add(new DeadChams())
                .add(new TotemAngle())
                .add(new Arrows())
                .add(new Particles())
                .add(new AuctionHelper())
                .add(new AutoMyst())
                .add(new AutoBuy())
                .add(new ChunkAnimator())
                .add(new AspectRatio())
                .add(new Saturation())
                .add(new TriggerBot())
                .add(new BowSpammer())
                .add(new Ambience())
                .add(new Fog())
                .add(new SkyShader())
                .add(new AutoTotem())
                .add(new TapeMouse())
                .add(new ElytraHelper())
                .add(new ChinaHat())
                .add(new Wings())
                .add(new AutoPotion())
                .add(new Jesus())
                .add(new ClientSounds())
                .add(new AutoGApple())
                .add(new ServerHelper())
                .add(new AutoExplosion())
                .add(new PacketCriticals())
                .add(new TargetESP())
                .add(new HitSound())
                .add(new ClickPearl())
                .add(new JumpCircle())
                .add(new ItemScroller())
                .add(new TargetStrafe())
                .add(new AutoLeave())
                .add(new GlassHands())
                .add(new HandShader())
                .add(new Strafe())
                .add(new AutoDuel())
                .add(new AutoMassage())
                .add(new DiscordRPCModule())
                .add(new NoWeb())
                .add(new AutoTpAccept())
                .add(new Spider())
                .add(new ClickFriend())
                .add(new FreeLook())
                .add(new Fly())
                .add(new ElytraMotion())
                .add(new FullBright())
                .add(new CameraSettings())
                .add(new ItemPhysic())
                .add(new NoDelay())
                .add(new ServerRPSpoofer())
                .add(new NoServerHead())
                .add(new DonateFix())
                .add(new SeeInvisible())
                .add(new AutoPilot())
                .add(new NoFallDamage())
                .add(new NoRender())
                .add(new HitBoxModule())
                .add(new WaterSpeed())
                .add(new NameProtect())
                .add(new NoFriendDamage())
                .add(new ProjectileHelper())
                .add(new InventoryMove())
                .add(new ChestStealer())
                .add(new NoInteract())
                .add(new AntiBot())
                .add(new ViewModel())
                .add(new HandMyItems())
                .add(new SuperFireWork())
                .add(new LongJump())
                .add(new ElytraTarget())
                .add(new FreeCam())
                .add(new Speed())
                .add(new NoEntityTrace())
                .add(new AutoRespawn())
                .add(new AutoSwap())
                .add(new NoPush())
                .add(new NoSlow())
                .add(new Velocity())
                //new
                .add(new ShieldDesync())
                .add(new ItemSwapFix())
                .add(new AirStuck())
                .add(new AutoJoiner())
                .add(new ElytraFlight())
                .add(new GrimGlide())
                .add(new BlockOverlay())
                //new
                .add(new SwingAnimation())
                .add(new AutoSprint())
                .hidden(new AutoParser())
                .hidden(new CosmeticsRuntime())
                .hidden(new NotificationRuntime());
    }

    public ModuleBuilder builder() {
        return new ModuleBuilder(this);
    }

    void registerModule(ModuleStructure module, boolean hidden) {
        Class<? extends ModuleStructure> clazz = module.getClass();
        if (registeredClasses.contains(clazz)) {
            throw new DuplicateModuleException(clazz.getSimpleName());
        }
        registeredClasses.add(clazz);
        if (hidden) {
            hiddenModules.add(module);
            module.setState(true);
        } else {
            moduleStructures.add(module);
        }
    }

    public List<ModuleStructure> modules() {
        return moduleStructures;
    }

    public List<ModuleStructure> hiddenModules() {
        return hiddenModules;
    }

    public List<ModuleStructure> allModules() {
        List<ModuleStructure> all = new ArrayList<>(moduleStructures);
        all.addAll(hiddenModules);
        return all;
    }
}
