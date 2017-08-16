package nl.lindooren.springreactive.google;

import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Value
public class Book {
    @NonNull
    String title;
    @NonNull
    Set<String> authors;
}
