package nl.lindooren.springreactive.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import nl.lindooren.springreactive.stats.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ItunesAlbumServiceImpl implements ItunesAlbumService {

    public static final String SERVICE_NAME = "albums";

    private WebClient webClient;
    private StatsService statsService;

    @Autowired
    public ItunesAlbumServiceImpl(ObjectMapper mapper, StatsService statsService) {
        this.statsService = statsService;
        // Itunes uses a legacy mime type for Json
        ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(clientCodecConfigurer ->
            clientCodecConfigurer.customCodecs().decoder(
                    new Jackson2JsonDecoder(mapper,
                            new MimeType("text", "javascript", StandardCharsets.UTF_8)))
        ).build();

        webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .baseUrl("https://itunes.apple.com")
                .build();
    }

    @Override
    public Flux<Album> searchAlbums(String query, int maxResults) {
        final long startTime = System.currentTimeMillis();
        return webClient.get()
                .uri("search?term={query}&media=music&entity=album&country=NL&limit={maxResults}", query, maxResults)
                .accept(new MediaType("text", "javascript", StandardCharsets.UTF_8))
                .retrieve()
                .bodyToMono(ItunesAlbumResponse.class)
                .doOnNext(googleBookResponse ->
                        statsService.notifyOfSuccessfulCall(SERVICE_NAME,
                                Optional.of(System.currentTimeMillis() - startTime)))
                .doOnError(throwable -> statsService.notifyOfFailedCall(SERVICE_NAME))
                .flatMapIterable(response -> response.results.orElseGet(ArrayList::new))
                .map(this::convertToAlbum);
    }

    private Album convertToAlbum(ItunesAlbumResponse.Result result) {
        return new Album(result.getCollectionName(), result.getArtistName());
    }

    /**
     * Using an inner class because we don't need to expose this to other services
     * (and it's small), so it's ok to keep it here
     */
    @Value
    private static class ItunesAlbumResponse {
        private Optional<List<Result>> results;

        @Value
        private static class Result {
            private String collectionName;
            private String artistName;
        }
    }
}
