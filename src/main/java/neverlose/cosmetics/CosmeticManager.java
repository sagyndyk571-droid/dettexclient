package neverlose.cosmetics;

import lombok.Getter;
import lombok.Setter;

public class CosmeticManager {
    @Getter
    private static final CosmeticManager instance = new CosmeticManager();

    @Getter
    private final CosmeticAnimator animator = new CosmeticAnimator();
    @Getter
    private final CosmeticRenderer renderer = new CosmeticRenderer();

    @Getter @Setter
    private CosmeticModel activeModel;
    @Getter @Setter
    private CosmeticModel activePet;
    @Getter @Setter
    private CosmeticModel activeAccessory;

    @Getter
    private boolean enabled = true;

    private boolean initialized;

    public synchronized void init() {
        CosmeticConfig.State saved = initialized ? null : CosmeticConfig.load();
        String modelId = activeModel != null ? activeModel.getId() : saved != null ? saved.model() : null;
        String petId = activePet != null ? activePet.getId() : saved != null ? saved.pet() : null;
        String accessoryId = activeAccessory != null ? activeAccessory.getId() : saved != null ? saved.accessory() : null;
        if (saved != null) {
            enabled = saved.enabled();
        }

        for (CosmeticModel model : CosmeticRepository.getInstance().all()) {
            model.resetTexture();
            model.clearRenderFault();
        }
        CosmeticRepository.getInstance().reload();
        animator.clearState();

        CosmeticRepository repo = CosmeticRepository.getInstance();
        activeModel = modelId != null ? repo.get(modelId) : null;
        if (activeModel == null && modelId != null && modelId.startsWith("builtin:model_")) {
            activeModel = repo.get(CosmeticModel.builtinCatalogId("model/goose"));
        }
        activePet = petId != null ? repo.get(petId) : null;
        activeAccessory = accessoryId != null ? repo.get(accessoryId) : null;

        initialized = true;
    }

    public void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    public CosmeticModel getActive(CosmeticType type) {
        ensureInitialized();
        return switch (type) {
            case PET -> activePet;
            case ACCESSORY -> activeAccessory;
            case MODEL -> activeModel;
        };
    }

    public void setActive(CosmeticType type, CosmeticModel model) {
        ensureInitialized();
        if (model != null) {
            CosmeticModel resolved = CosmeticRepository.getInstance().get(model.getId());
            if (resolved != null) {
                model = resolved;
            }
            model.clearRenderFault();
        }
        switch (type) {
            case PET -> activePet = model;
            case ACCESSORY -> activeAccessory = model;
            case MODEL -> activeModel = model;
        }
        CosmeticConfig.save(this);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        CosmeticConfig.save(this);
    }

    public void clear(CosmeticType type) {
        setActive(type, null);
    }

    public boolean hasAnyActive() {
        return activeModel != null || activePet != null || activeAccessory != null;
    }

    public boolean isActive() {
        return activeModel != null;
    }
}

