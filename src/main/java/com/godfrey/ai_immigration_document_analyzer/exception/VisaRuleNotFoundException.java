package com.godfrey.ai_immigration_document_analyzer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**

 * Exception thrown when a visa rule is not found for a given country and visa type.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class VisaRuleNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE =
            "Visa rule not found";

    private final String country;
    private final String visaType;

    public VisaRuleNotFoundException() {
        super(DEFAULT_MESSAGE);
        this.country = null;
        this.visaType = null;
    }

    public VisaRuleNotFoundException(String country, String visaType) {
        super(buildMessage(country, visaType));
        this.country = country;
        this.visaType = visaType;
    }

    public VisaRuleNotFoundException(String country, String visaType, Throwable cause) {
        super(buildMessage(country, visaType), cause);
        this.country = country;
        this.visaType = visaType;
    }

    public VisaRuleNotFoundException(String message) {
        super(message);
        this.country = null;
        this.visaType = null;
    }

    public VisaRuleNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.country = null;
        this.visaType = null;
    }

    private static String buildMessage(String country, String visaType) {
        return "Visa rule not found for country: "
                + country
                + " and visa type: "
                + visaType;
    }

    public String getCountry() {
        return country;
    }

    public String getVisaType() {
        return visaType;
    }
}
