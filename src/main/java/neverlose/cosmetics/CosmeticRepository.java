package neverlose.cosmetics;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CosmeticRepository {
    private static final CosmeticRepository INSTANCE = new CosmeticRepository();
    private static final Logger LOGGER = LoggerFactory.getLogger("neverlose/Cosmetics");
    private static final int MAX_ARCHIVE_ENTRIES = 4096;
    private static final long MAX_ARCHIVE_SIZE = 256L * 1024L * 1024L;

    private final Map<String, CosmeticModel> catalog = new LinkedHashMap<>();

    public static CosmeticRepository getInstance() {
        return INSTANCE;
    }

    public void reload() {
        catalog.clear();
        discoverBuiltIn();
        discoverUserFolder(Path.of(CosmeticPaths.COSMETICS_DIRECTORY.toString()));
    }

    public int importFiguraAvatars() {
        int before = catalog.size();
        discoverFiguraAvatars();
        return catalog.size() - before;
    }

    public List<CosmeticModel> all() {
        return new ArrayList<>(catalog.values());
    }

    public List<CosmeticModel> byType(CosmeticType type) {
        return catalog.values().stream().filter(m -> m.getType() == type).toList();
    }

    public CosmeticModel get(String id) {
        return catalog.get(id);
    }

    private void register(CosmeticModel model) {
        if (model != null && !catalog.containsKey(model.getId())) {
            catalog.put(model.getId(), model);
        }
    }

    private static final List<String> BUNDLED_MODELS = List.of(
            "model/goose", "model/starhorm",
            "accessory/simplewings",
            "pet/allay", "pet/mothli", "pet/birb"
    );

    private void discoverBuiltIn() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getResourceManager() != null) {
            try {
                mc.getResourceManager().findResources("cosmetic", id -> id.getPath().endsWith("/avatar.json"))
                        .forEach((id, resource) -> {
                            String path = id.getPath().toLowerCase();
                            if (!path.startsWith("cosmetic/") || !path.endsWith("/avatar.json")) return;
                            String resourcePath = path.substring("cosmetic/".length(), path.length() - "/avatar.json".length());
                            CosmeticType forced = typeFromResourcePath(resourcePath);
                            register(CosmeticModel.loadBuiltin(resourcePath, forced));
                        });
            } catch (Exception ignored) {
            }
        }

        for (String folder : BUNDLED_MODELS) {
            String id = CosmeticModel.builtinCatalogId(folder);
            if (!catalog.containsKey(id)) {
                CosmeticType forced = typeFromResourcePath(folder);
                register(CosmeticModel.loadBuiltin(folder, forced));
            }
        }
    }

    private static CosmeticType typeFromResourcePath(String resourcePath) {
        if (resourcePath == null) return null;
        int slash = resourcePath.indexOf('/');
        if (slash <= 0) return null;
        return CosmeticType.fromJson(resourcePath.substring(0, slash));
    }

    private void discoverUserFolder(Path root) {
        if (!Files.isDirectory(root)) {
            try {
                Files.createDirectories(root);
                for (CosmeticType type : CosmeticType.values()) {
                    Files.createDirectories(root.resolve(type.getId().toLowerCase()));
                }
            } catch (Exception ignored) {
            }
            return;
        }

        for (CosmeticType type : CosmeticType.values()) {
            Path typeDir = root.resolve(type.getId().toLowerCase());
            importZipArchives(typeDir);
            scanDirectory(typeDir, type, "user");
        }
        importZipArchives(root);
        scanDirectory(root, null, "user");
    }

    private void discoverFiguraAvatars() {
        Path figura = Path.of(CosmeticPaths.GAME_DIRECTORY.toString(), "figura", "avatars");
        if (!Files.isDirectory(figura)) return;

        try (Stream<Path> stream = Files.list(figura)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                String key = "figura:" + dir.getFileName().toString().toLowerCase();
                if (catalog.containsKey(key)) return;
                CosmeticModel model = CosmeticModel.loadExternal(dir.toFile(), "figura");
                if (model != null) {
                    model.setSource("Figura");
                    register(model);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void scanDirectory(Path dir, CosmeticType forcedType, String sourceTag) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> stream = Files.walk(dir, 4)) {
            stream.filter(Files::isDirectory)
                    .filter(sub -> !sub.equals(dir))
                    .filter(sub -> !hasModelAncestor(sub.getParent(), dir))
                    .forEach(sub -> {
                File folder = sub.toFile();
                if (new File(folder, "avatar.json").exists() || hasBbmodel(folder)) {
                    CosmeticModel model = CosmeticModel.loadExternal(folder, sourceTag);
                    if (model != null) {
                        if (forcedType != null && model.getType() == CosmeticType.MODEL) {
                            model = model.withType(forcedType);
                        }
                        register(model);
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static boolean hasBbmodel(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".bbmodel"));
        return files != null && files.length > 0;
    }

    private static boolean hasModelAncestor(Path current, Path scanRoot) {
        while (current != null && !current.equals(scanRoot)) {
            if (hasBbmodel(current.toFile()) || Files.isRegularFile(current.resolve("avatar.json"))) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void importZipArchives(Path directory) {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .forEach(this::importZipArchive);
        } catch (IOException error) {
            LOGGER.warn("Failed to scan cosmetic archives in {}", directory, error);
        }
    }

    private void importZipArchive(Path archive) {
        String fileName = archive.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - 4).trim();
        if (baseName.isEmpty()) baseName = "imported_model";

        Path destination = archive.getParent().resolve(baseName).normalize();
        if (!destination.getParent().equals(archive.getParent().normalize())) return;
        if (Files.isDirectory(destination) && containsModel(destination)) return;

        Path temporary = archive.getParent().resolve("." + baseName + ".importing").normalize();
        try {
            deleteTree(temporary);
            Files.createDirectories(temporary);
            extractArchive(archive, temporary);
            if (!containsModel(temporary)) {
                throw new IOException("archive does not contain avatar.json or a bbmodel");
            }
            if (Files.exists(destination)) deleteTree(destination);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.info("Imported cosmetic archive {}", archive.getFileName());
        } catch (Exception error) {
            try {
                deleteTree(temporary);
            } catch (IOException ignored) {
            }
            LOGGER.warn("Failed to import cosmetic archive {}", archive, error);
        }
    }

    private static void extractArchive(Path archive, Path destination) throws IOException {
        int entries = 0;
        long totalBytes = 0L;
        byte[] buffer = new byte[8192];

        try (InputStream input = Files.newInputStream(archive);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ARCHIVE_ENTRIES) throw new IOException("too many archive entries");
                Path output = destination.resolve(entry.getName()).normalize();
                if (!output.startsWith(destination)) throw new IOException("unsafe archive path");

                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }
                Files.createDirectories(output.getParent());
                try (OutputStream file = Files.newOutputStream(output)) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        totalBytes += read;
                        if (totalBytes > MAX_ARCHIVE_SIZE) throw new IOException("archive is too large");
                        file.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private static boolean containsModel(Path directory) {
        if (!Files.isDirectory(directory)) return false;
        try (Stream<Path> files = Files.walk(directory, 3)) {
            return files.anyMatch(path -> Files.isRegularFile(path)
                    && (path.getFileName().toString().equalsIgnoreCase("avatar.json")
                    || path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel")));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static void openCosmeticsFolder() {
        try {
            File dir = new File(CosmeticPaths.COSMETICS_DIRECTORY.toString());
            if (!dir.exists()) dir.mkdirs();
            String cmd = "explorer \"" + dir.getAbsolutePath() + "\"";
            Runtime.getRuntime().exec(cmd);
        } catch (Exception ignored) {
        }
    }
}

