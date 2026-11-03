package com.sistemaventas.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ilustraciones vectoriales de artículos de ferretería, dibujadas sobre un lienzo de 100×100
 * para que se vean nítidas en cualquier tamaño y sin depender de archivos externos.
 */
public final class ToolArt {
    private static final Color STEEL = new Color(0xADB5BD);
    private static final Color STEEL_DARK = new Color(0x6C757D);
    private static final Color STEEL_LIGHT = new Color(0xDEE2E6);
    private static final Color RUBBER = new Color(0x343A40);
    private static final Color ORANGE = new Color(0xF76707);
    private static final Color YELLOW = new Color(0xFAB005);
    private static final Color RED = new Color(0xE03131);
    private static final Color BLUE = new Color(0x1C7ED6);
    private static final Color GREEN = new Color(0x2F9E44);
    private static final Color WOOD = new Color(0xC08552);
    private static final Color BRASS = new Color(0xE0A82E);
    private static final Color OUTLINE = new Color(0x212529);

    /** Clave → nombre que se muestra al elegir una ilustración. */
    public static final Map<String, String> CATALOG = catalog();

    private ToolArt() {
    }

    private static Map<String, String> catalog() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("martillo", "Martillo");
        names.put("destornillador", "Destornillador");
        names.put("pinza", "Pinza");
        names.put("llave", "Llave combinada");
        names.put("llave_francesa", "Llave francesa");
        names.put("serrucho", "Serrucho");
        names.put("cinta_metrica", "Cinta métrica");
        names.put("nivel", "Nivel");
        names.put("caja", "Caja de herramientas");
        names.put("taladro", "Taladro");
        names.put("amoladora", "Amoladora");
        names.put("tornillos", "Tornillos");
        names.put("silicona", "Silicona / sellador");
        names.put("cinta_aisladora", "Cinta aisladora");
        names.put("foco", "Lámpara");
        names.put("cable", "Cable");
        names.put("canilla", "Canilla / grifería");
        names.put("pintura", "Lata de pintura");
        names.put("pincel", "Pincel");
        names.put("rodillo", "Rodillo");
        names.put("guantes", "Guantes");
        names.put("anteojos", "Anteojos de seguridad");
        names.put("candado", "Candado");
        names.put("escalera", "Escalera");
        names.put("generico", "Genérico");
        return java.util.Collections.unmodifiableMap(names);
    }

    public static BufferedImage image(String key, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        paint(g, key, 0, 0, size);
        g.dispose();
        return image;
    }

    public static void paint(Graphics2D target, String key, double x, double y, double size) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.scale(size / 100.0, size / 100.0);
        shadow(g);
        switch (key == null ? "" : key) {
            case "martillo" -> hammer(g);
            case "destornillador" -> screwdriver(g);
            case "pinza" -> pliers(g);
            case "llave" -> wrench(g);
            case "llave_francesa" -> adjustableWrench(g);
            case "serrucho" -> saw(g);
            case "cinta_metrica" -> tapeMeasure(g);
            case "nivel" -> level(g);
            case "caja" -> toolbox(g);
            case "taladro" -> drill(g);
            case "amoladora" -> grinder(g);
            case "tornillos" -> screws(g);
            case "silicona" -> sealant(g);
            case "cinta_aisladora" -> electricalTape(g);
            case "foco" -> bulb(g);
            case "cable" -> cable(g);
            case "canilla" -> faucet(g);
            case "pintura" -> paintCan(g);
            case "pincel" -> brush(g);
            case "rodillo" -> roller(g);
            case "guantes" -> gloves(g);
            case "anteojos" -> glasses(g);
            case "candado" -> padlock(g);
            case "escalera" -> ladder(g);
            default -> generic(g);
        }
        g.dispose();
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static void shadow(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 28));
        g.fill(new Ellipse2D.Double(20, 86, 60, 8));
    }

    private static void fill(Graphics2D g, Shape shape, Color color) {
        g.setColor(color);
        g.fill(shape);
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    private static void shade(Graphics2D g, Shape shape, Color top, Color bottom, Rectangle2D bounds) {
        g.setPaint(new GradientPaint((float) bounds.getX(), (float) bounds.getY(), top,
                (float) bounds.getX(), (float) bounds.getMaxY(), bottom));
        g.fill(shape);
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    private static Shape rr(double x, double y, double w, double h, double arc) {
        return new RoundRectangle2D.Double(x, y, w, h, arc, arc);
    }

    private static Shape circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
    }

    private static Path2D poly(double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        path.closePath();
        return path;
    }

    private static Graphics2D rotated(Graphics2D g, double degrees) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.rotate(Math.toRadians(degrees), 50, 50);
        return copy;
    }

    private static void highlight(Graphics2D g, Shape shape) {
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(shape);
    }

    // ---- herramientas manuales ---------------------------------------------------------------

    private static void hammer(Graphics2D base) {
        Graphics2D g = rotated(base, 35);
        fill(g, rr(45, 30, 10, 62, 6), YELLOW);
        fill(g, rr(44, 62, 12, 30, 7), RUBBER);
        for (int y = 66; y < 90; y += 5) {
            g.setColor(new Color(255, 255, 255, 40));
            g.fill(new Rectangle2D.Double(45, y, 10, 2));
        }
        Area head = new Area(rr(22, 16, 40, 18, 4));
        Path2D claw = new Path2D.Double();
        claw.moveTo(60, 17);
        claw.curveTo(70, 17, 78, 23, 82, 36);
        claw.lineTo(77, 38);
        claw.curveTo(73, 30, 67, 28, 60, 30);
        claw.closePath();
        head.add(new Area(claw));
        shade(g, head, STEEL_LIGHT, STEEL_DARK, new Rectangle2D.Double(0, 10, 0, 30));
        fill(g, rr(18, 15, 8, 20, 3), STEEL_DARK);
        g.dispose();
    }

    private static void screwdriver(Graphics2D base) {
        Graphics2D g = rotated(base, 40);
        fill(g, rr(48, 8, 4, 42, 2), STEEL);
        fill(g, poly(46, 10, 54, 10, 52, 4, 48, 4), STEEL_DARK);
        fill(g, rr(44, 46, 12, 8, 3), STEEL_LIGHT);
        Shape handle = rr(39, 52, 22, 40, 12);
        shade(g, handle, new Color(0xFF6B6B), new Color(0xC92A2A), new Rectangle2D.Double(0, 52, 0, 40));
        g.setColor(new Color(0xFFD43B));
        g.fill(rr(43, 58, 3, 28, 2));
        g.fill(rr(54, 58, 3, 28, 2));
        highlight(g, rr(42, 54, 6, 14, 6));
        g.dispose();
    }

    private static void pliers(Graphics2D base) {
        Graphics2D g = rotated(base, 0);
        Path2D left = new Path2D.Double();
        left.moveTo(47, 46);
        left.curveTo(40, 60, 30, 76, 26, 92);
        left.lineTo(34, 94);
        left.curveTo(40, 78, 48, 62, 53, 50);
        left.closePath();
        Path2D right = new Path2D.Double();
        right.moveTo(53, 46);
        right.curveTo(60, 60, 70, 76, 74, 92);
        right.lineTo(66, 94);
        right.curveTo(60, 78, 52, 62, 47, 50);
        right.closePath();
        fill(g, left, RED);
        fill(g, right, YELLOW);
        Area jaws = new Area(poly(42, 46, 44, 18, 50, 8, 56, 18, 58, 46));
        jaws.subtract(new Area(new Rectangle2D.Double(49.3, 10, 1.4, 30)));
        shade(g, jaws, STEEL_LIGHT, STEEL_DARK, new Rectangle2D.Double(0, 8, 0, 40));
        g.setColor(STEEL_DARK);
        for (int y = 22; y < 36; y += 4) g.fill(new Rectangle2D.Double(44, y, 12, 1.2));
        fill(g, circle(50, 46, 5), STEEL);
        fill(g, circle(50, 46, 1.8), STEEL_DARK);
        g.dispose();
    }

    private static void wrench(Graphics2D base) {
        Graphics2D g = rotated(base, -40);
        Area shape = new Area(rr(44, 22, 12, 58, 6));
        shape.add(new Area(circle(50, 18, 14)));
        shape.subtract(new Area(new Rectangle2D.Double(44, 2, 12, 16)));
        shape.add(new Area(circle(50, 82, 12)));
        Path2D hex = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i + 30);
            double px = 50 + 6.5 * Math.cos(angle);
            double py = 82 + 6.5 * Math.sin(angle);
            if (i == 0) hex.moveTo(px, py); else hex.lineTo(px, py);
        }
        hex.closePath();
        shape.subtract(new Area(hex));
        shade(g, shape, STEEL_LIGHT, STEEL_DARK, new Rectangle2D.Double(0, 4, 0, 90));
        highlight(g, rr(47, 30, 3, 40, 3));
        g.dispose();
    }

    private static void adjustableWrench(Graphics2D base) {
        Graphics2D g = rotated(base, -40);
        Area shape = new Area(rr(43, 34, 14, 56, 10));
        shape.add(new Area(circle(50, 26, 17)));
        shape.subtract(new Area(rr(43, 4, 14, 22, 3)));
        shade(g, shape, STEEL_LIGHT, STEEL_DARK, new Rectangle2D.Double(0, 8, 0, 82));
        fill(g, rr(41, 32, 18, 7, 2), STEEL);
        g.setColor(STEEL_DARK);
        for (int x = 43; x < 58; x += 3) g.fill(new Rectangle2D.Double(x, 33, 1.2, 5));
        fill(g, circle(50, 82, 3.5), STEEL_DARK);
        highlight(g, rr(46, 44, 3, 30, 3));
        g.dispose();
    }

    private static void saw(Graphics2D base) {
        Graphics2D g = rotated(base, -12);
        Path2D blade = new Path2D.Double();
        blade.moveTo(10, 42);
        blade.lineTo(66, 38);
        blade.lineTo(66, 62);
        double x = 66;
        boolean down = true;
        while (x > 12) {
            x -= 3;
            blade.lineTo(x, (down ? 66 : 62) - (66 - x) * 0.11);
            down = !down;
        }
        blade.lineTo(10, 56);
        blade.closePath();
        shade(g, blade, STEEL_LIGHT, STEEL, new Rectangle2D.Double(0, 38, 0, 30));
        Area handle = new Area(rr(62, 32, 28, 38, 16));
        handle.subtract(new Area(rr(70, 42, 12, 18, 9)));
        fill(g, handle, new Color(0x5C940D));
        fill(g, circle(66, 42, 2), STEEL_DARK);
        fill(g, circle(66, 58, 2), STEEL_DARK);
        g.dispose();
    }

    private static void tapeMeasure(Graphics2D g) {
        fill(g, rr(70, 66, 24, 10, 2), YELLOW);
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1f));
        for (int x = 72; x < 92; x += 3) g.draw(new java.awt.geom.Line2D.Double(x, 66, x, x % 2 == 0 ? 71 : 69));
        fill(g, rr(91, 64, 4, 14, 1), STEEL);
        Shape body = rr(16, 22, 60, 60, 22);
        shade(g, body, new Color(0xFFD43B), new Color(0xF08C00), new Rectangle2D.Double(0, 22, 0, 60));
        fill(g, rr(16, 56, 60, 26, 14), RUBBER);
        fill(g, circle(46, 46, 14), RUBBER);
        fill(g, circle(46, 46, 6), STEEL);
        fill(g, rr(38, 18, 16, 8, 3), RED);
        highlight(g, rr(22, 26, 24, 10, 10));
    }

    private static void level(Graphics2D base) {
        Graphics2D g = rotated(base, -8);
        Shape body = rr(6, 38, 88, 24, 6);
        shade(g, body, new Color(0xFFD43B), new Color(0xF59F00), new Rectangle2D.Double(0, 38, 0, 24));
        fill(g, rr(6, 38, 10, 24, 6), RUBBER);
        fill(g, rr(84, 38, 10, 24, 6), RUBBER);
        fill(g, rr(38, 43, 24, 14, 6), new Color(0xD8F5A2));
        fill(g, new Ellipse2D.Double(46, 46, 8, 7), new Color(0xF8F9FA));
        g.setColor(OUTLINE);
        g.draw(new java.awt.geom.Line2D.Double(44, 43, 44, 57));
        g.draw(new java.awt.geom.Line2D.Double(56, 43, 56, 57));
        fill(g, circle(26, 50, 5), new Color(0xD8F5A2));
        fill(g, circle(74, 50, 5), new Color(0xD8F5A2));
        g.dispose();
    }

    private static void toolbox(Graphics2D g) {
        Area handle = new Area(rr(34, 14, 32, 16, 8));
        handle.subtract(new Area(rr(39, 19, 22, 11, 5)));
        fill(g, handle, RUBBER);
        Shape body = rr(12, 40, 76, 46, 8);
        shade(g, body, new Color(0xFF6B6B), new Color(0xC92A2A), new Rectangle2D.Double(0, 40, 0, 46));
        Shape lid = rr(10, 26, 80, 18, 8);
        shade(g, lid, new Color(0x495057), new Color(0x212529), new Rectangle2D.Double(0, 26, 0, 18));
        fill(g, rr(20, 40, 10, 10, 2), STEEL_LIGHT);
        fill(g, rr(70, 40, 10, 10, 2), STEEL_LIGHT);
        highlight(g, rr(16, 48, 40, 6, 6));
    }

    // ---- herramientas eléctricas -------------------------------------------------------------

    private static void drill(Graphics2D g) {
        fill(g, rr(78, 42, 18, 5, 2), STEEL);
        g.setColor(STEEL_DARK);
        for (int x = 80; x < 95; x += 4) g.fill(poly(x, 42, x + 2, 42, x + 3, 47, x + 1, 47));
        fill(g, rr(66, 36, 14, 17, 4), RUBBER);
        Path2D grip = new Path2D.Double();
        grip.moveTo(34, 52);
        grip.lineTo(52, 52);
        grip.lineTo(46, 80);
        grip.lineTo(28, 80);
        grip.closePath();
        fill(g, grip, RUBBER);
        fill(g, rr(50, 52, 5, 9, 2), RED);
        Shape body = rr(16, 26, 54, 30, 14);
        shade(g, body, new Color(0x4DABF7), new Color(0x1864AB), new Rectangle2D.Double(0, 26, 0, 30));
        fill(g, rr(20, 78, 36, 12, 4), RUBBER);
        fill(g, rr(22, 72, 32, 8, 3), BLUE);
        g.setColor(new Color(255, 255, 255, 120));
        for (int x = 22; x < 34; x += 4) g.fill(rr(x, 34, 2, 14, 2));
        highlight(g, rr(24, 28, 34, 6, 6));
    }

    private static void grinder(Graphics2D g) {
        fill(g, rr(8, 44, 18, 12, 6), RUBBER);
        Shape body = rr(20, 38, 46, 24, 12);
        shade(g, body, new Color(0x69DB7C), new Color(0x2B8A3E), new Rectangle2D.Double(0, 38, 0, 24));
        fill(g, rr(26, 42, 14, 16, 4), RUBBER);
        g.setColor(new Color(255, 255, 255, 110));
        for (int x = 28; x < 39; x += 3) g.fill(rr(x, 44, 1.5, 12, 1));
        fill(g, rr(60, 40, 14, 20, 4), STEEL_DARK);
        fill(g, circle(76, 64, 20), new Color(0x495057));
        fill(g, circle(76, 64, 14), new Color(0x868E96));
        fill(g, circle(76, 64, 4), STEEL_LIGHT);
        Area guard = new Area(new Arc2D.Double(52, 40, 48, 48, 20, 160, Arc2D.PIE));
        guard.subtract(new Area(circle(76, 64, 17)));
        fill(g, guard, STEEL);
        fill(g, rr(60, 22, 8, 20, 4), RUBBER);
    }

    // ---- fijaciones --------------------------------------------------------------------------

    private static void screw(Graphics2D base, double x, double y, double degrees, double scale) {
        Graphics2D g = (Graphics2D) base.create();
        g.translate(x, y);
        g.rotate(Math.toRadians(degrees));
        g.scale(scale, scale);
        Path2D shaft = poly(-4, 0, 4, 0, 4, 34, 0, 44, -4, 34);
        shade(g, shaft, STEEL_LIGHT, STEEL_DARK, new Rectangle2D.Double(0, 0, 0, 44));
        g.setColor(STEEL_DARK);
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 4; i < 36; i += 4) g.draw(new java.awt.geom.Line2D.Double(-4.5, i, 4.5, i + 2.5));
        fill(g, new Ellipse2D.Double(-10, -6, 20, 9), STEEL_LIGHT);
        g.setColor(STEEL_DARK);
        g.fill(new Rectangle2D.Double(-1, -5, 2, 6.5));
        g.dispose();
    }

    private static void screws(Graphics2D g) {
        Shape box = rr(14, 58, 72, 30, 6);
        shade(g, box, new Color(0xFFA94D), new Color(0xE8590C), new Rectangle2D.Double(0, 58, 0, 30));
        g.setColor(new Color(255, 255, 255, 200));
        g.fill(rr(24, 66, 52, 14, 4));
        g.setColor(OUTLINE);
        g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 10f));
        g.drawString("x100", 39, 77);
        screw(g, 30, 22, -18, 0.9);
        screw(g, 52, 12, 0, 1.0);
        screw(g, 72, 22, 20, 0.9);
    }

    private static void sealant(Graphics2D base) {
        Graphics2D g = rotated(base, -30);
        fill(g, poly(46, 16, 54, 16, 51, -2, 49, -2), new Color(0xF8F9FA));
        fill(g, rr(44, 14, 12, 6, 2), STEEL_LIGHT);
        Shape tube = rr(34, 18, 32, 70, 6);
        shade(g, tube, Color.WHITE, new Color(0xCED4DA), new Rectangle2D.Double(0, 18, 0, 70));
        fill(g, rr(34, 38, 32, 30, 2), BLUE);
        g.setColor(Color.WHITE);
        g.fill(rr(38, 46, 24, 4, 2));
        g.fill(rr(38, 54, 16, 3, 2));
        fill(g, rr(32, 84, 36, 6, 2), STEEL);
        g.dispose();
    }

    // ---- electricidad ------------------------------------------------------------------------

    private static void electricalTape(Graphics2D g) {
        fill(g, rr(44, 78, 46, 6, 2), new Color(0x343A40));
        fill(g, circle(48, 52, 32), new Color(0x2B2F33));
        g.setColor(new Color(255, 255, 255, 50));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Arc2D.Double(22, 26, 52, 52, 100, 80, Arc2D.OPEN));
        g.draw(new Arc2D.Double(28, 32, 40, 40, 100, 70, Arc2D.OPEN));
        fill(g, circle(48, 52, 14), new Color(0xE9ECEF));
        fill(g, circle(48, 52, 10), new Color(0xF8F9FA));
    }

    private static void bulb(Graphics2D g) {
        g.setColor(new Color(255, 212, 59, 70));
        g.fill(circle(50, 38, 34));
        Area glass = new Area(circle(50, 38, 24));
        glass.add(new Area(poly(36, 52, 64, 52, 58, 68, 42, 68)));
        shade(g, glass, new Color(0xFFF9DB), new Color(0xFFE066), new Rectangle2D.Double(0, 14, 0, 54));
        g.setColor(new Color(0xF08C00));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D filament = new Path2D.Double();
        filament.moveTo(45, 62);
        filament.lineTo(45, 44);
        filament.curveTo(47, 36, 53, 36, 55, 44);
        filament.lineTo(55, 62);
        g.draw(filament);
        for (int i = 0; i < 3; i++) fill(g, rr(41, 67 + i * 6, 18, 6, 3), STEEL);
        fill(g, poly(44, 85, 56, 85, 52, 90, 48, 90), RUBBER);
        highlight(g, new Ellipse2D.Double(36, 22, 10, 14));
    }

    private static void cable(Graphics2D g) {
        for (int i = 0; i < 5; i++) {
            double inset = i * 5.5;
            Shape ring = new Ellipse2D.Double(14 + inset, 22 + inset * 0.8, 72 - inset * 2, 60 - inset * 1.6);
            g.setColor(OUTLINE);
            g.setStroke(new BasicStroke(8f));
            g.draw(ring);
            g.setColor(i % 2 == 0 ? RED : new Color(0xFA5252));
            g.setStroke(new BasicStroke(6f));
            g.draw(ring);
        }
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(80, 60, 94, 76));
        g.setColor(RED);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(80, 60, 92, 74));
        fill(g, circle(94, 77, 3), new Color(0xE8590C));
    }

    // ---- plomería ----------------------------------------------------------------------------

    private static void faucet(Graphics2D g) {
        fill(g, rr(20, 84, 60, 8, 4), STEEL_DARK);
        Shape column = rr(54, 34, 16, 52, 6);
        shade(g, column, STEEL_LIGHT, STEEL_DARK, new Rectangle2D.Double(54, 0, 16, 0));
        Area spout = new Area(new Arc2D.Double(18, 18, 52, 44, 0, 180, Arc2D.PIE));
        spout.subtract(new Area(new Arc2D.Double(28, 28, 32, 28, 0, 180, Arc2D.PIE)));
        spout.add(new Area(rr(18, 38, 10, 10, 3)));
        shade(g, spout, STEEL_LIGHT, STEEL, new Rectangle2D.Double(0, 18, 0, 30));
        fill(g, rr(60, 12, 26, 7, 3), STEEL);
        fill(g, rr(58, 16, 10, 20, 4), STEEL_LIGHT);
        g.setColor(new Color(0x74C0FC));
        g.fill(rr(20, 50, 6, 10, 3));
        g.fill(rr(21, 64, 4, 6, 2));
        highlight(g, rr(57, 40, 4, 40, 3));
    }

    // ---- pinturería --------------------------------------------------------------------------

    private static void paintCan(Graphics2D g) {
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(new Arc2D.Double(24, 8, 52, 46, 20, 140, Arc2D.OPEN));
        Shape body = rr(22, 28, 56, 60, 8);
        shade(g, body, STEEL_LIGHT, STEEL, new Rectangle2D.Double(0, 28, 0, 60));
        fill(g, new Ellipse2D.Double(22, 22, 56, 14), new Color(0xE9ECEF));
        fill(g, rr(22, 44, 56, 30, 2), new Color(0x4C6EF5));
        g.setColor(Color.WHITE);
        g.fill(poly(30, 44, 44, 44, 42, 56, 38, 52, 34, 58, 32, 50));
        g.fill(rr(48, 54, 24, 5, 2));
        g.fill(rr(48, 62, 16, 4, 2));
    }

    private static void brush(Graphics2D base) {
        Graphics2D g = rotated(base, 30);
        Shape handle = rr(43, 4, 14, 44, 7);
        shade(g, handle, new Color(0xE0A16B), new Color(0x9C6232), new Rectangle2D.Double(43, 0, 14, 0));
        fill(g, circle(50, 11, 2.5), new Color(0x6F4518));
        fill(g, rr(38, 46, 24, 14, 3), STEEL);
        Path2D bristles = poly(38, 60, 62, 60, 64, 86, 36, 86);
        fill(g, bristles, new Color(0xF1D4A8));
        fill(g, poly(36, 78, 64, 78, 64, 86, 36, 86), new Color(0x4C6EF5));
        g.dispose();
    }

    private static void roller(Graphics2D g) {
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D frame = new Path2D.Double();
        frame.moveTo(84, 32);
        frame.lineTo(90, 32);
        frame.lineTo(90, 50);
        frame.lineTo(54, 56);
        frame.lineTo(54, 66);
        g.draw(frame);
        Shape cylinder = rr(12, 18, 74, 28, 12);
        shade(g, cylinder, new Color(0xFFA94D), new Color(0xE8590C), new Rectangle2D.Double(0, 18, 0, 28));
        g.setColor(new Color(255, 255, 255, 60));
        for (int x = 18; x < 82; x += 6) g.fill(rr(x, 22, 2, 20, 2));
        fill(g, rr(46, 64, 16, 30, 7), RUBBER);
    }

    // ---- seguridad ---------------------------------------------------------------------------

    private static void gloves(Graphics2D g) {
        Color leather = new Color(0xE8B87A);
        Area glove = new Area(rr(26, 40, 44, 40, 16));
        glove.add(new Area(rr(26, 16, 10, 34, 10)));
        glove.add(new Area(rr(37, 10, 10, 38, 10)));
        glove.add(new Area(rr(48, 12, 10, 36, 10)));
        glove.add(new Area(rr(59, 20, 10, 30, 10)));
        Path2D thumb = new Path2D.Double();
        thumb.moveTo(28, 64);
        thumb.curveTo(14, 58, 8, 46, 14, 40);
        thumb.curveTo(20, 36, 26, 46, 32, 54);
        thumb.closePath();
        glove.add(new Area(thumb));
        shade(g, glove, new Color(0xF3D2A2), leather, new Rectangle2D.Double(0, 10, 0, 70));
        Shape cuff = rr(24, 76, 48, 16, 4);
        fill(g, cuff, BLUE);
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(rr(24, 82, 48, 3, 1));
        g.setColor(new Color(0xB07A3E));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2, 2}, 0));
        g.draw(new java.awt.geom.Line2D.Double(30, 56, 66, 56));
    }

    private static void glasses(Graphics2D g) {
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(10, 40, 20, 46));
        g.draw(new java.awt.geom.Line2D.Double(90, 40, 80, 46));
        Path2D lens = new Path2D.Double();
        lens.moveTo(14, 42);
        lens.curveTo(30, 34, 70, 34, 86, 42);
        lens.curveTo(88, 60, 74, 70, 62, 66);
        lens.curveTo(56, 64, 54, 56, 50, 56);
        lens.curveTo(46, 56, 44, 64, 38, 66);
        lens.curveTo(26, 70, 12, 60, 14, 42);
        lens.closePath();
        g.setColor(new Color(116, 192, 252, 150));
        g.fill(lens);
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(lens);
        fill(g, rr(14, 36, 72, 7, 4), YELLOW);
        g.setColor(new Color(255, 255, 255, 160));
        g.fill(poly(22, 46, 30, 46, 24, 58, 20, 54));
        g.fill(poly(62, 46, 70, 46, 64, 58, 60, 54));
    }

    private static void padlock(Graphics2D g) {
        Area shackle = new Area(rr(28, 10, 44, 56, 44));
        shackle.subtract(new Area(rr(36, 18, 28, 50, 28)));
        shackle.subtract(new Area(new Rectangle2D.Double(20, 40, 60, 40)));
        shackle.add(new Area(new Rectangle2D.Double(28, 34, 8, 12)));
        shackle.add(new Area(new Rectangle2D.Double(64, 34, 8, 12)));
        shade(g, shackle, STEEL_LIGHT, STEEL_DARK, new Rectangle2D.Double(28, 0, 44, 0));
        Shape body = rr(20, 42, 60, 46, 10);
        shade(g, body, new Color(0xFFD43B), BRASS, new Rectangle2D.Double(0, 42, 0, 46));
        fill(g, circle(50, 60, 6), RUBBER);
        fill(g, poly(47, 62, 53, 62, 55, 76, 45, 76), RUBBER);
        highlight(g, rr(25, 46, 14, 36, 8));
    }

    private static void ladder(Graphics2D g) {
        Shape left = poly(30, 8, 38, 8, 26, 94, 16, 94);
        Shape right = poly(62, 8, 70, 8, 84, 94, 74, 94);
        for (int i = 0; i < 6; i++) {
            double y = 18 + i * 13;
            double inset = (y - 8) * 0.14;
            fill(g, rr(34 - inset, y, 32 + inset * 2, 6, 2), STEEL);
        }
        shade(g, left, STEEL_LIGHT, STEEL, new Rectangle2D.Double(16, 0, 22, 0));
        shade(g, right, STEEL_LIGHT, STEEL, new Rectangle2D.Double(62, 0, 22, 0));
        fill(g, rr(14, 90, 14, 6, 2), RUBBER);
        fill(g, rr(72, 90, 14, 6, 2), RUBBER);
    }

    private static void generic(Graphics2D g) {
        Graphics2D left = rotated(g, 45);
        fill(left, rr(46, 14, 8, 74, 4), STEEL);
        left.dispose();
        Graphics2D right = rotated(g, -45);
        fill(right, rr(46, 14, 8, 74, 4), new Color(0xE8590C));
        right.dispose();
        fill(g, circle(50, 50, 7), RUBBER);
    }

    public static List<String> keys() {
        return List.copyOf(CATALOG.keySet());
    }
}
