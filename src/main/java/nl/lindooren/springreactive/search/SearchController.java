package nl.lindooren.springreactive.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
public class SearchController {

    private SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * <pre>
     *     curl 'http://localhost:8080/search/albumsAndBooks/Bicycle'
     * </pre>
     *
     * @param query
     * @return
     */
    @GetMapping(value = "/search/albumsAndBooks/{query}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Flux<MediaItem> searchMedia(@PathVariable String query) {
        return searchService.searchAlbumsAndBooks(query);
    }
}
