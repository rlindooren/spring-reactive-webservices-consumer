package nl.lindooren.springreactive.google;

import reactor.core.publisher.Flux;

/**
 * Uses Google Books API to find books matching the given query string
 *
 * @see <a href="https://developers.google.com/books/docs/v1/reference/volumes/list">Google Books API</a>
 */
public interface GoogleBookService {

    /**
     * @param query      Full-text search query string
     * @param maxResults the maximum number of results to return
     * @return books that match the given query string
     */
    Flux<Book> searchBooks(final String query, final int maxResults);
}
