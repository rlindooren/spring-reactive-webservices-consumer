package nl.lindooren.springreactive.stats;

import lombok.Value;

import java.util.Optional;

/**
 * Represents a single (individual) call to a webservice
 */
@Value
public class WebserviceCallEvent {
    private String webserviceName;
    private boolean success;
    private Optional<Long> responseTime;
}
