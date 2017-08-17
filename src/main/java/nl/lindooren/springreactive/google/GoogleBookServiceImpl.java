package nl.lindooren.springreactive.google;

import lombok.Value;
import nl.lindooren.springreactive.stats.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GoogleBookServiceImpl implements GoogleBookService {

    public static final String SERVICE_NAME = "books";

    private WebClient webClient;
    private StatsService statsService;

    @Autowired
    public GoogleBookServiceImpl(StatsService statsService) {
        this.statsService = statsService;
        webClient = WebClient.create("https://www.googleapis.com");
    }

    @Override
    public Flux<Book> searchBooks(String query, int maxResults) {
        final long startTime = System.currentTimeMillis();
        return webClient.get()
                .uri("/books/v1/volumes?q={query}&maxResults={maxResults}", query, maxResults)
                .acceptCharset(StandardCharsets.UTF_8)
                .retrieve()
                .bodyToMono(GoogleBookResponse.class)
                .doOnNext(googleBookResponse ->
                        statsService.notifyOfSuccessfulCall(SERVICE_NAME,
                                Optional.of(System.currentTimeMillis() - startTime)))
                .doOnError(throwable -> statsService.notifyOfFailedCall(SERVICE_NAME))
                .flatMapIterable(response -> response.items.orElseGet(ArrayList::new))
                .map(this::convertToBook);
    }

    private Book convertToBook(GoogleBookResponse.Item item) {
        return new Book(item.volumeInfo.getTitle(), item.volumeInfo.getAuthors().orElse(Collections.emptySet()));
    }

    /**
     * Using an inner class because we don't need to expose this to other services
     * (and it's small), so it's ok to keep it here
     */
    @Value
    private static class GoogleBookResponse {
        private Optional<List<Item>> items;

        @Value
        private static class Item {
            private VolumeInfo volumeInfo;

            @Value
            private static class VolumeInfo {
                private String title;
                private Optional<Set<String>> authors;
            }
        }
    }
}
