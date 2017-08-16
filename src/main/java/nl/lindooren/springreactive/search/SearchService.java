package nl.lindooren.springreactive.search;

import reactor.core.publisher.Flux;

/**
 * This service tries to find books and albums matching a given search query
 */
public interface SearchService {

    Flux<MediaItem> searchAlbumsAndBooks(final String query);
}
