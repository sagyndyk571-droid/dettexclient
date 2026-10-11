package neverlose.manager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final ConcurrentHashMap<String, Identifier> CACHE = new ConcurrentHashMap<>();

    /**
     * Возвращает идентификатор текстуры для указанного URL.
     */
    public static Identifier getOrLoad(String url) {
        if (url == null || url.isEmpty()) return null;
        if (CACHE.containsKey(url)) return CACHE.get(url);
        // Асинхронная загрузка
        CompletableFuture.runAsync(() -> {
            try {
                BufferedImage img = ImageIO.read(new URL(url));
                if (img != null) {
                    NativeImage nativeImage = convertToNativeImage(img);
                    Identifier identifier = Identifier.of("neverlose", "cover_" + System.currentTimeMillis());
                    MinecraftClient.getInstance().getTextureManager().registerTexture(identifier);
                    CACHE.put(url, identifier);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return null; // Сначала вернём null, через кадр будет доступно
    }

    private static NativeImage convertToNativeImage(BufferedImage image) {
        NativeImage out = new NativeImage(image.getWidth(), image.getHeight(), true);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                out.setColor(x, y, abgr);
            }
        }
        return out;
    }
}