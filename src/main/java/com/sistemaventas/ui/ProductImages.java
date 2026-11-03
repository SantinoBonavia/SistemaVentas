package com.sistemaventas.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * Resuelve la imagen de un producto: "art:clave" usa una ilustración incorporada; cualquier otro
 * valor es la ruta de una foto. Si la foto falta, cae en una ilustración genérica.
 */
public final class ProductImages {
    public static final String ART_PREFIX = "art:";
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_PHOTO_SIZE = 600;

    private ProductImages() {
    }

    public static BufferedImage image(String key, int size) {
        String safeKey = key == null || key.isBlank() ? ART_PREFIX + "generico" : key;
        return CACHE.computeIfAbsent(safeKey + "@" + size, ignored -> render(safeKey, size));
    }

    public static boolean isPhoto(String key) {
        return key != null && !key.isBlank() && !key.startsWith(ART_PREFIX);
    }

    /** Copia la foto elegida a la carpeta de datos, reducida, y devuelve la clave a guardar. */
    public static String importPhoto(Path source, Path photosDirectory) throws IOException {
        BufferedImage original = ImageIO.read(source.toFile());
        if (original == null) {
            throw new IOException("El archivo no es una imagen compatible (JPG, PNG, GIF o BMP).");
        }
        double scale = Math.min(1.0, (double) MAX_PHOTO_SIZE / Math.max(original.getWidth(), original.getHeight()));
        int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(original.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        Files.createDirectories(photosDirectory);
        Path target = photosDirectory.resolve("foto-" + System.currentTimeMillis() + ".png");
        ImageIO.write(resized, "png", target.toFile());
        return target.toAbsolutePath().toString();
    }

    private static BufferedImage render(String key, int size) {
        if (key.startsWith(ART_PREFIX)) {
            return ToolArt.image(key.substring(ART_PREFIX.length()), size);
        }
        try {
            BufferedImage photo = ImageIO.read(Path.of(key).toFile());
            if (photo != null) {
                return cover(photo, size);
            }
        } catch (IOException | RuntimeException ignored) {
            // Foto borrada o ilegible: se muestra la ilustración genérica.
        }
        return ToolArt.image("generico", size);
    }

    /** Recorta al centro para llenar un cuadrado, como hacen los catálogos online. */
    private static BufferedImage cover(BufferedImage photo, int size) {
        double scale = Math.max((double) size / photo.getWidth(), (double) size / photo.getHeight());
        int width = (int) Math.round(photo.getWidth() * scale);
        int height = (int) Math.round(photo.getHeight() * scale);
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, size, size, size * 0.12, size * 0.12));
        g.drawImage(photo, (size - width) / 2, (size - height) / 2, width, height, null);
        g.dispose();
        return result;
    }
}
