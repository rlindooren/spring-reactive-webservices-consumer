package nl.lindooren.springreactive.stats;

import lombok.extern.slf4j.Slf4j;
import org.pcollections.TreePVector;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class StatsServiceImpl implements StatsService {

    private static final Duration STATS_INTERVAL = Duration.ofSeconds(5);
    private UnicastProcessor<WebserviceCallEvent> processor;
    private Flux<WebserviceCallEvent> hotEvents;
    private Map<String, Flux<WebserviceStats>> statsForWebservices;

    @PostConstruct
    private void init() {
        statsForWebservices = new ConcurrentHashMap<>();
        processor = UnicastProcessor.create();
        hotEvents = processor.publish().autoConnect().doOnNext(this::startCollectingStatsForNewWebservice);
        // This subscribe will trigger the onNext without having to wait on 'real' subscribers
        hotEvents.subscribe();
    }

    @Override
    public void notifyOfSuccessfulCall(final String serviceName, final Optional<Long> responseTime) {
        processor.onNext(new WebserviceCallEvent(serviceName, true, responseTime));
    }

    @Override
    public void notifyOfFailedCall(String serviceName) {
        processor.onNext(new WebserviceCallEvent(serviceName, false, Optional.empty()));
    }

    @Override
    public Flux<WebserviceCallEvent> getEventStreamForAllWebservices() {
        return hotEvents;
    }

    @Override
    public Flux<WebserviceCallEvent> createEventStreamForWebservice(final String webserviceName) {
        return hotEvents.filter(event -> event.getWebserviceName().equals(webserviceName));
    }

    @Override
    public Optional<Flux<WebserviceStats>> getStatsStreamForWebservice(String webserviceName) {
        return Optional.ofNullable(statsForWebservices.get(webserviceName));
    }

    @Override
    public Optional<WebserviceStats> getCurrentStatsForWebservice(String webserviceName) {
        return Optional.ofNullable(
                statsForWebservices
                        .getOrDefault(webserviceName, Flux.empty())
                        .take(1).singleOrEmpty()
                        .take(Duration.ofMillis(50)).block()
        );
    }

    @Override
    public boolean areAllWebservicesConsideredHealthy() {
        return statsForWebservices.keySet().stream().map(s -> getCurrentStatsForWebservice(s))
                .allMatch(webserviceStats -> webserviceStats.isPresent() && webserviceStats.get().isHealthy());
    }

    private void startCollectingStatsForNewWebservice(WebserviceCallEvent webserviceCallEvent) {
        final String webserviceName = webserviceCallEvent.getWebserviceName();
        if (!statsForWebservices.containsKey(webserviceName)) {
            // Start collecting statistics on given interval for this webservice
            Flux<WebserviceStats> statsStream = createEventStreamForWebservice(webserviceName)
                    .window(STATS_INTERVAL)
                    .flatMap(webserviceCallEventFlux ->
                            webserviceCallEventFlux.reduce(
                                    createInitialWebserviceStatsForAggregation(),
                                    this::aggregateWebserviceCallEvents
                            )
                    ).map(Tuple2::getT1)
                    .cache(1);
            statsForWebservices.put(webserviceName, statsStream);
            log.info("Created aggregation stream for webservice: {}", webserviceName);
            // Use a noop subscriber to have the stream start emitting events even when there's no 'real' subscriber yet
            statsStream.subscribe();
        }
    }

    /**
     * Poor man's aggregation attempt, the focus here is immutability
     *
     * @param stateWithAllResponseTimes the current aggregation state with all response times
     * @param event                     the next event in the stream to aggregate
     */
    private Tuple2<WebserviceStats, TreePVector<Long>> aggregateWebserviceCallEvents(
            Tuple2<WebserviceStats, TreePVector<Long>> stateWithAllResponseTimes,
            WebserviceCallEvent event) {

        WebserviceStats currentState = stateWithAllResponseTimes.getT1();
        TreePVector<Long> responseTimes = stateWithAllResponseTimes.getT2();

        final long newNrOfRequests = currentState.getNrOfRequests() + 1;
        final long newNrOfErrors = currentState.getNrOfErrors() + (event.isSuccess() ? 0 : 1);
        final double errorRate = ((double)newNrOfErrors * 100) / newNrOfRequests;

        WebserviceStats newState =
                currentState
                        .withNrOfRequests(newNrOfRequests)
                        .withNrOfErrors(newNrOfErrors)
                        .withRequestsPerSecond((double) newNrOfRequests / currentState.getWindowSize().getSeconds())
                        .withErrorRate(errorRate)
                        // Very simplistic way of determining the health of this webservice
                        .withHealthy(errorRate < 10);

        if (event.getResponseTime().isPresent()) {
            final long responseTime = event.getResponseTime().get();
            responseTimes = responseTimes.plus(responseTime);
            LongSummaryStatistics longSummary = responseTimes.stream().mapToLong(value -> value).summaryStatistics();
            newState = newState
                    .withAvgResponseTime((long) longSummary.getAverage())
                    .withMaxResponseTime(longSummary.getMax())
                    .withMinResponseTime(longSummary.getMin());
        }

        return Tuples.of(newState, responseTimes);
    }

    private Tuple2<WebserviceStats, TreePVector<Long>> createInitialWebserviceStatsForAggregation() {
        return Tuples.of(
                new WebserviceStats(true, STATS_INTERVAL, 0, 0, 0, 0, 0, 0, 0),
                TreePVector.empty()
        );
    }
}
