package nl.lindooren.springreactive.search;

import lombok.extern.slf4j.Slf4j;
import nl.lindooren.springreactive.apple.Album;
import nl.lindooren.springreactive.apple.ItunesAlbumService;
import nl.lindooren.springreactive.apple.ItunesAlbumServiceImpl;
import nl.lindooren.springreactive.google.Book;
import nl.lindooren.springreactive.google.GoogleBookService;
import nl.lindooren.springreactive.google.GoogleBookServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import static nl.lindooren.springreactive.search.MediaItem.Type;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    /**
     * This service is expected to return a result within one second.
     * Therefore we wait max 900ms on both services (note: not sequentially)
     * allowing 100ms for the response to be created (which should be more than enough).
     */
    private static final Duration MAX_WAIT_TIME_ON_THIRD_PARTY = Duration.ofMillis(900);

    private GoogleBookService googleBookService;
    private ItunesAlbumService itunesAlbumService;
    private int nrOfResultsPerType;

    @Autowired
    public SearchServiceImpl(GoogleBookService googleBookService, ItunesAlbumService itunesAlbumService,
                             @Value("${search.albumsAndBooks.nrOfResultsPerType}") int nrOfResultsPerType) {
        this.googleBookService = googleBookService;
        this.itunesAlbumService = itunesAlbumService;
        this.nrOfResultsPerType = nrOfResultsPerType;
    }

    @Override
    public Flux<MediaItem> searchAlbumsAndBooks(final String query) {
        final String querySanitized = query.toLowerCase();

        Flux<MediaItem> albums = configureTimeoutAndErrorBehaviour(
                itunesAlbumService.searchAlbums(querySanitized, nrOfResultsPerType).map(this::convertToMediaItem),
                ItunesAlbumServiceImpl.SERVICE_NAME, querySanitized);

        Flux<MediaItem> books = configureTimeoutAndErrorBehaviour(
                googleBookService.searchBooks(querySanitized, nrOfResultsPerType).map(this::convertToMediaItem),
                GoogleBookServiceImpl.SERVICE_NAME, querySanitized);

        return albums.mergeWith(books).sort();
    }

    private Flux<MediaItem> configureTimeoutAndErrorBehaviour(
            Flux<MediaItem> flux, final String serviceName, final String query) {
        return flux
                // Note: capturing stats here instead of on the webclient(s) would allow to also
                // catch the time-out (cancellation) and log it as webservice failure.
                .timeout(MAX_WAIT_TIME_ON_THIRD_PARTY)
                .doOnError(TimeoutException.class,
                        e -> log.warn("The {} service took too long to return results", serviceName))
                .doOnError(t -> !(t instanceof TimeoutException),
                        t -> log.error("Unexpected error while consuming the " + serviceName + " service " +
                                "for query '" + query + "'", t))
                // On error return an empty result.
                // Note: this may cause the client to think there were no matches for the given query
                // while in fact an error occurred.
                // Another option would be to propagate the error down stream,
                // letting the client handle it with a circuit breaker for example
                .onErrorResume(throwable -> Flux.empty());
    }

    private MediaItem convertToMediaItem(Book book) {
        return new MediaItem(book.getTitle(), book.getAuthors(), Type.BOOK);
    }

    private MediaItem convertToMediaItem(Album album) {
        return new MediaItem(album.getTitle(), Collections.singleton(album.getAuthor()), Type.ALBUM);
    }
}
