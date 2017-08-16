package nl.lindooren.springreactive.search;

import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Value
public class MediaItem implements Comparable<MediaItem> {
    @NonNull
    private String title;
    @NonNull
    private Set<String> authors;
    @NonNull
    private Type type;

    /**
     * By default sort on title
     *
     * @param o the other {@link MediaItem}
     * @return
     */
    @Override
    public int compareTo(MediaItem o) {
        return title.compareTo(o.title);
    }

    enum Type {
        BOOK,
        ALBUM
    }
}
