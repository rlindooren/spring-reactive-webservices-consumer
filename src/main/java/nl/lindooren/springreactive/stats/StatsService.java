package nl.lindooren.springreactive.stats;

import reactor.core.publisher.Flux;

import java.util.Optional;

public interface StatsService {

    void notifyOfSuccessfulCall(final String serviceName, final Optional<Long> responseTime);

    void notifyOfFailedCall(final String serviceName);

    Flux<WebserviceCallEvent> getEventStreamForAllWebservices();

    Flux<WebserviceCallEvent> createEventStreamForWebservice(final String webserviceName);

    Optional<Flux<WebserviceStats>> getStatsStreamForWebservice(final String webserviceName);

    Optional<WebserviceStats> getCurrentStatsForWebservice(final String webserviceName);

    boolean areAllWebservicesConsideredHealthy();
}
