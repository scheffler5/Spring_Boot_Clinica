package com.learn.projeto_learn.service.validation;

import com.learn.projeto_learn.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
public class EmailValidationService {


    @Value("${email.domain.check:true}")
    private boolean domainCheckEnabled;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "mailinator.com",
            "guerrillamail.com", "guerrillamail.info", "guerrillamail.net",
            "guerrillamail.org", "guerrillamail.de", "guerrillamailblock.com",
            "tempmail.com", "temp-mail.org", "temp-mail.io", "tempinbox.com",
            "throwam.com", "throwaway.email",
            "yopmail.com", "yopmail.fr",
            "cool.fr.nf", "jetable.fr.nf", "nospam.ze.tc", "nomail.xl.cx",
            "mega.zik.dj", "speed.1s.fr", "courriel.fr.nf",
            "moncourrier.fr.nf", "monemail.fr.nf", "monmail.fr.nf",
            "sharklasers.com", "spam4.me",
            "trashmail.com", "trashmail.at", "trashmail.io", "trashmail.me",
            "trashmail.net", "trashmail.org",
            "maildrop.cc", "dispostable.com", "fakeinbox.com", "mailnull.com",
            "spamgourmet.com", "spamgourmet.org", "spamgourmet.net",
            "10minutemail.com", "10minutemail.net", "20minutemail.com",
            "mintemail.com", "discard.email", "tempr.email",
            "incognitomail.com", "mailfreeonline.com",
            "spamherelots.com", "spamhereplease.com", "crazymailing.com",
            "mohmal.com", "spamhole.com", "mytrashmail.com",
            "sogetthis.com", "spamthisplease.com", "fuckingduh.com",
            "hulapla.de", "dontsendmespam.de", "nospamfor.us",
            "trashdevil.com", "trashdevil.de",
            "mailexpire.com", "spamtrail.com", "filzmail.com",
            "getairmail.com", "gowaymail.com", "fakemailgenerator.com",
            "binkmail.com", "bobmail.info", "chammy.info", "devnullmail.com",
            "letthemeatspam.com", "mailinater.com", "mailinator2.com",
            "notmailinator.com", "suremail.info",
            "mailbolt.com", "mailc.net", "mailchop.com", "mailfall.com",
            "mailimate.com", "mailme.lv", "mailsiphon.com",
            "tempemail.net", "tempsky.com"
    );

    public void validate(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("E-mail não pode ser vazio.");
        }
        if (!EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException("Formato de e-mail inválido.");
        }
        if (domainCheckEnabled) {
            String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();
            if (DISPOSABLE_DOMAINS.contains(domain)) {
                throw new BusinessException("E-mails temporários ou descartáveis não são permitidos.");
            }
        }
    }

    public boolean isDisposable(String email) {
        if (!domainCheckEnabled) return false;
        if (email == null || email.isBlank()) return false;
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();
        return DISPOSABLE_DOMAINS.contains(domain);
    }
}
