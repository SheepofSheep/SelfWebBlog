package org.example.selfwebblog.interaction;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class GuestTrustService {
    private static final Pattern LINK = Pattern.compile("(?i)(https?://|www\\.|[a-z0-9-]+\\.(com|net|org|cn))");

    public String statusFor(boolean previouslyApproved, String content, boolean rateSuspicious) {
        if (!previouslyApproved || rateSuspicious || LINK.matcher(content == null ? "" : content).find()) {
            return "PENDING";
        }
        return "PUBLISHED";
    }
}
