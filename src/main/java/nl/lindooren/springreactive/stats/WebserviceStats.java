package nl.lindooren.springreactive.stats;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

import java.time.Duration;

@Value
@Wither
@AllArgsConstructor
public class WebserviceStats {
    private boolean healthy;
    private Duration windowSize;
    private long nrOfRequests;
    private long nrOfErrors;
    private double errorRate;
    private double requestsPerSecond;
    private long minResponseTime;
    private long maxResponseTime;
    private long avgResponseTime;
}
