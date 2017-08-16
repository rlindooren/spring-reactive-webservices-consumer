package nl.lindooren.springreactive.apple;

import reactor.core.publisher.Flux;

/**
 * Uses Itunes API to find albums matching the given query string
 *
 * @see <a href="https://affiliate.itunes.apple.com/resources/documentation/itunes-store-web-service-search-api/#searching">Itunes API</a>
 */
public interface ItunesAlbumService {

    /**
     * @param query      Full-text search query string
     * @param maxResults the maximum number of results to return
     * @return albums that match the given query string
     */
    Flux<Album> searchAlbums(final String query, final int maxResults);
}
