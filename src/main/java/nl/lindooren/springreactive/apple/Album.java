package nl.lindooren.springreactive.apple;

import lombok.NonNull;
import lombok.Value;

@Value
public class Album {
    @NonNull
    String title;
    @NonNull
    String author;
}
