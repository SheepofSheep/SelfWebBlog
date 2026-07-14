package org.example.selfwebblog.identity.captcha;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class CaptchaService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> PURPOSES = Set.of("guestbook", "register", "login");

    private final Cache<String, Challenge> challenges;
    private final Supplier<String> answerGenerator;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService() {
        this(Ticker.systemTicker(), Duration.ofMinutes(5), null);
    }

    CaptchaService(Ticker ticker, Duration expiry, Supplier<String> answerGenerator) {
        this.challenges = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(expiry.toNanos(), TimeUnit.NANOSECONDS)
                .ticker(ticker)
                .build();
        this.answerGenerator = answerGenerator != null ? answerGenerator : this::randomAnswer;
    }

    public CaptchaResponse create(String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        String id = UUID.randomUUID().toString();
        String answer = answerGenerator.get().toUpperCase(Locale.ROOT);
        challenges.put(id, new Challenge(normalizedPurpose, answer));
        return new CaptchaResponse(id, render(answer));
    }

    public boolean verify(String challengeId, String answer, String purpose) {
        if (challengeId == null || answer == null) return false;
        Challenge challenge = challenges.asMap().remove(challengeId);
        if (challenge == null) return false;
        return challenge.purpose().equals(normalizePurpose(purpose))
                && challenge.answer().equals(answer.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizePurpose(String purpose) {
        String normalized = purpose == null ? "" : purpose.trim().toLowerCase(Locale.ROOT);
        if (!PURPOSES.contains(normalized)) throw new IllegalArgumentException("Unsupported captcha purpose");
        return normalized;
    }

    private String randomAnswer() {
        StringBuilder answer = new StringBuilder(5);
        for (int index = 0; index < 5; index++) {
            answer.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return answer.toString();
    }

    private String render(String answer) {
        try {
            BufferedImage image = new BufferedImage(150, 50, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(255, 250, 236));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            for (int index = 0; index < 7; index++) {
                graphics.setColor(new Color(170, 120, 35, 90));
                graphics.drawLine(random.nextInt(150), random.nextInt(50), random.nextInt(150), random.nextInt(50));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
            for (int index = 0; index < answer.length(); index++) {
                graphics.setColor(index % 2 == 0 ? new Color(93, 66, 22) : new Color(177, 108, 21));
                graphics.drawString(String.valueOf(answer.charAt(index)), 14 + index * 25, 35 + random.nextInt(5));
            }
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render captcha", exception);
        }
    }

    private record Challenge(String purpose, String answer) {
    }
}
