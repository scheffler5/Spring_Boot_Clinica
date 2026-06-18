package com.learn.projeto_learn.service.captcha;

import com.learn.projeto_learn.dto.captcha.CaptchaResponseDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int WIDTH = 220;
    private static final int HEIGHT = 70;
    private static final int EXPIRATION_MINUTES = 5;

    private final SecureRandom rng = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        cleaner.scheduleAtFixedRate(this::evict, 5, 5, TimeUnit.MINUTES);
    }

    public CaptchaResponseDTO generate() {
        String code = randomCode();
        String id = UUID.randomUUID().toString();
        store.put(id, new Entry(code, LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES)));
        return new CaptchaResponseDTO(id, renderToBase64(code));
    }

    public boolean validate(String id, String userInput) {
        Entry entry = store.remove(id);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) return false;
        return entry.code().equalsIgnoreCase(userInput != null ? userInput.trim() : "");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        return sb.toString();
    }

    private String renderToBase64(String code) {
        System.setProperty("java.awt.headless", "true");

        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GradientPaint bg = new GradientPaint(0, 0, new Color(232, 238, 250), WIDTH, HEIGHT, new Color(212, 222, 244));
        g.setPaint(bg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setStroke(new BasicStroke(0.9f));
        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(140 + rng.nextInt(90), 140 + rng.nextInt(90), 170 + rng.nextInt(60)));
            g.drawLine(rng.nextInt(WIDTH), rng.nextInt(HEIGHT),
                       rng.nextInt(WIDTH), rng.nextInt(HEIGHT));
        }

        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(160 + rng.nextInt(70), 160 + rng.nextInt(70), 190 + rng.nextInt(50)));
            g.setStroke(new BasicStroke(1.2f));
            g.drawArc(rng.nextInt(WIDTH / 2), rng.nextInt(HEIGHT),
                      40 + rng.nextInt(80), 20 + rng.nextInt(40),
                      rng.nextInt(360), 90 + rng.nextInt(180));
        }

        String[] fonts = {"Arial", "Verdana", "Tahoma", "Courier New"};
        int slotW = WIDTH / (CODE_LENGTH + 1);
        for (int i = 0; i < code.length(); i++) {
            Font f = new Font(fonts[rng.nextInt(fonts.length)], Font.BOLD, 26 + rng.nextInt(10));
            g.setFont(f);
            g.setColor(new Color(10 + rng.nextInt(80), 10 + rng.nextInt(80), 80 + rng.nextInt(100)));

            int x = slotW * (i + 1) - slotW / 3;
            int y = HEIGHT / 2 + 9 + rng.nextInt(8) - 4;
            double angle = Math.toRadians(rng.nextInt(36) - 18);

            AffineTransform orig = g.getTransform();
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.setTransform(orig);
        }

        for (int i = 0; i < 80; i++) {
            g.setColor(new Color(rng.nextInt(200), rng.nextInt(200), rng.nextInt(200)));
            g.fillOval(rng.nextInt(WIDTH), rng.nextInt(HEIGHT), 2, 2);
        }

        g.setColor(new Color(170, 182, 210));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "PNG", baos);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar imagem CAPTCHA", e);
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private void evict() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record Entry(String code, LocalDateTime expiresAt) {}
}
