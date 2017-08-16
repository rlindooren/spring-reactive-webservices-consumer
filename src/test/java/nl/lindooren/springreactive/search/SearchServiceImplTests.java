package nl.lindooren.springreactive.search;

import nl.lindooren.springreactive.apple.Album;
import nl.lindooren.springreactive.apple.ItunesAlbumService;
import nl.lindooren.springreactive.google.Book;
import nl.lindooren.springreactive.google.GoogleBookService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SearchServiceImplTests {

    Book book1 = new Book("To Kill a Mockingbird", new HashSet<>(Arrays.asList("Harper Lee")));
    Book book2 = new Book("Gone with the Wind", new HashSet<>(Arrays.asList("Margaret Mitchell")));

    Flux<Book> unsortedBooks = Flux.just(book1, book2);

    Album album1 = new Album("Starboy","The Weeknd");
    Album album2 = new Album("Evolve","Imagine Dragons");

    Flux<Album> unsortedAlbums = Flux.just(album1, album2);

    @Test
    public void testSorting() {
        final String query = "foo"; // Ignored
        final int nrOfResults = 5; // Ignored

        GoogleBookService googleBookService = mock(GoogleBookService.class);
        when(googleBookService.searchBooks(query, nrOfResults)).thenReturn(unsortedBooks);

        ItunesAlbumService itunesAlbumService = mock(ItunesAlbumService.class);
        when(itunesAlbumService.searchAlbums(query, nrOfResults)).thenReturn(unsortedAlbums);

        SearchServiceImpl searchService = new SearchServiceImpl(googleBookService, itunesAlbumService, nrOfResults);
        List<MediaItem> items = searchService.searchAlbumsAndBooks(query)
                .collectList().block();

        assertThat(items).containsExactly(
                new MediaItem(album2.getTitle(), Collections.singleton(album2.getAuthor()), MediaItem.Type.ALBUM),
                new MediaItem(book2.getTitle(), book2.getAuthors(), MediaItem.Type.BOOK),
                new MediaItem(album1.getTitle(), Collections.singleton(album1.getAuthor()), MediaItem.Type.ALBUM),
                new MediaItem(book1.getTitle(), book1.getAuthors(), MediaItem.Type.BOOK)
        );
    }

    @Test
    public void testTimeoutResilience() {
        final String query = "foo"; // Ignored
        final int nrOfResults = 5; // Ignored

        GoogleBookService googleBookService = mock(GoogleBookService.class);
        when(googleBookService.searchBooks(query, nrOfResults)).thenReturn(unsortedBooks);

        // Return albums only after 2 seconds (the current search implementation waits max 1 second)
        unsortedAlbums = unsortedAlbums.delayElements(Duration.ofSeconds(2));
        ItunesAlbumService itunesAlbumService = mock(ItunesAlbumService.class);
        when(itunesAlbumService.searchAlbums(query, nrOfResults)).thenReturn(unsortedAlbums);

        SearchServiceImpl searchService = new SearchServiceImpl(googleBookService, itunesAlbumService, nrOfResults);
        List<MediaItem> items = searchService.searchAlbumsAndBooks(query)
                .collectList().block();

        assertThat(items).containsExactly(
                new MediaItem(book2.getTitle(), book2.getAuthors(), MediaItem.Type.BOOK),
                new MediaItem(book1.getTitle(), book1.getAuthors(), MediaItem.Type.BOOK)
        );
    }

    @Test
    public void testErrorResilience() {
        final String query = "foo"; // Ignored
        final int nrOfResults = 5; // Ignored

        // Cause a failure while processing books
        unsortedBooks = unsortedBooks.doOnNext(book -> {throw new RuntimeException("Something went wrong");});
        GoogleBookService googleBookService = mock(GoogleBookService.class);
        when(googleBookService.searchBooks(query, nrOfResults)).thenReturn(unsortedBooks);

        ItunesAlbumService itunesAlbumService = mock(ItunesAlbumService.class);
        when(itunesAlbumService.searchAlbums(query, nrOfResults)).thenReturn(unsortedAlbums);

        SearchServiceImpl searchService = new SearchServiceImpl(googleBookService, itunesAlbumService, nrOfResults);
        List<MediaItem> items = searchService.searchAlbumsAndBooks(query)
                .collectList().block();

        assertThat(items).containsExactly(
                new MediaItem(album2.getTitle(), Collections.singleton(album2.getAuthor()), MediaItem.Type.ALBUM),
                new MediaItem(album1.getTitle(), Collections.singleton(album1.getAuthor()), MediaItem.Type.ALBUM)
        );
    }
}
