package nl.lindooren.springreactive.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class StatsController {

    private StatsService statsService;

    /**
     * Web Socket emitting information about actual webservice calls made to all upstream services
     * <pre>
     *     curl -H "Connection: Upgrade" -H "Upgrade: websocket" 'http://localhost:8080/monitor/events/stream'
     * </pre>
     *
     * @return a stream of all calls that are made to upstream webservices
     */
    @GetMapping(value = "/monitor/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WebserviceCallEvent> getEventsWebsocketStream() {
        return statsService.getEventStreamForAllWebservices();
    }

    /**
     * Web Socket emitting information about actual webservice calls made to a specific upstream services
     * <pre>
     *     curl -H "Connection: Upgrade" -H "Upgrade: websocket" 'http://localhost:8080/monitor/events/books/stream'
     *     curl -H "Connection: Upgrade" -H "Upgrade: websocket" 'http://localhost:8080/monitor/events/albums/stream'
     * </pre>
     *
     * @return a stream of all calls that are made to a specific upstream webservices
     */
    @GetMapping(value = "/monitor/events/{webserviceName}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WebserviceCallEvent> getEventsWebsocketStream(@PathVariable String webserviceName) {
        return statsService.createEventStreamForWebservice(webserviceName);
    }

    /**
     * Web Socket emitting aggregated statistics of calls made to a specific upstream services during a certain period
     * <pre>
     *     curl -H "Connection: Upgrade" -H "Upgrade: websocket" 'http://localhost:8080/monitor/stats/books/stream'
     *     curl -H "Connection: Upgrade" -H "Upgrade: websocket" 'http://localhost:8080/monitor/stats/albums/stream'
     * </pre>
     *
     * @return a stream of aggregated information about calls that are made to a specific upstream webservice
     */
    @GetMapping(value = "/monitor/stats/{webserviceName}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WebserviceStats> getStatsWebsocketStream(@PathVariable String webserviceName) {
        return statsService.getStatsStreamForWebservice(webserviceName)
                // TODO: this doesn't work for a websocket connection
                // The connection to (and initiated by) the client isn't closed.
                // I guess the error is just send down stream in the reactive handler
                // but I need to investigate this further.
                .orElseThrow(StatsNotAvailableException::new);
    }

    /**
     * Current status of aggregated statistics of calls made to a specific upstream services during the last period.
     * <p>
     * <pre>
     *     curl 'http://localhost:8080/monitor/stats/books'
     *     curl 'http://localhost:8080/monitor/stats/albums'
     * </pre>
     *
     * @return a stream of aggregated information about calls that are made to a specific upstream webservice <br/>
     * or 204 No Content if no call to the upstream webservice hasn't been made yet.
     */
    @GetMapping(value = "/monitor/stats/{webserviceName}")
    public ResponseEntity<WebserviceStats> getStats(@PathVariable String webserviceName) {
        return statsService.getCurrentStatsForWebservice(webserviceName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * <pre>
     *     curl 'http://localhost:8080/health'
     * </pre>
     *
     * @return a response with status code 200 if healthy (none 200 otherwise)
     */
    @GetMapping(value = "/health")
    public ResponseEntity<String> getHealth() {
        if (statsService.areAllWebservicesConsideredHealthy()) {
            return ResponseEntity.ok("No webservice is unhealthy. Ready to go!");
        } else {
            // Don't respond with 200 (for most load balancers etc this is the initial check 200 = OK)
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("There's an unhealthy webservice");
        }
    }

    @Autowired
    public void setStatsService(StatsService statsService) {
        this.statsService = statsService;
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY, reason = "The statistics are not available yet (a call has to be made first to the webservice)")
    private class StatsNotAvailableException extends RuntimeException {
        private StatsNotAvailableException() {
            super("The statistics are not available yet (a call has to be made first to the webservice)");
        }
    }
}
