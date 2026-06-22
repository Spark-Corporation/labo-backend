package org.sparkcorporation.lims.order.infra;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the scheduled destruction-alert scan (CDC Onglet « Conservation »). A blank
 * {@code alert-recipient} disables the email push (the in-app queue still serves the same data).
 */
@Getter
@Setter
@ConfigurationProperties("lims.conservation")
public class ConservationAlertProperties {

    /** Cron for the destruction-alert scan (default wired in application.yml). */
    private String alertCron = "0 0 7 * * *";

    /** Ops email that receives the overdue-destruction digest; blank → push disabled. */
    private String alertRecipient = "";
}
