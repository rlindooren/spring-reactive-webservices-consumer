## About this project

I created this project to investigate WebFlux; Spring's reactive web framework.

In this case a search service allows searching for two different types of media.
Each media type is collected from a different webservice.

The retrieval of results from each webservice is done in paralel as a stream (Flux).
Both streams are merged into one stream which is then sent to the client.

The search service will always answer within one second.
Discarding results from a webservice that took too long to answer.
Errors from one of the upstream webservices will be swallowed and not propegated to the client.
If both webservices would be in error, then the client would receive an empty JSON array.

At the moment Hystrix isn't available yet for WebFlux. 
Which led me to implement a very simple statistics service. Also based on reactive streams.

## Examples

The application can be started like this:
```bash
mvn spring-boot:run
```

#### Search for media

_'Bicycle' in this case_

```bash
curl 'http://localhost:8080/search/albumsAndBooks/Bicycle'
```
possible response:
```json
[{"title":"A Different Kind of Fix","authors":["Bombay Bicycle Club"],"type":"ALBUM"},
{"title":"Bicycle","authors":["Bicycle"],"type":"ALBUM"},
{"title":"Bicycle","authors":["David V. Herlihy"],"type":"BOOK"},
{"title":"Bicycle","authors":["Intertec Publishing Corporation. Abos Marine Publications Division"],"type":"BOOK"},
{"title":"Bicycle Science Projects","authors":["Robert Gardner"],"type":"BOOK"},
{"title":"Flaws","authors":["Bombay Bicycle Club"],"type":"ALBUM"},
{"title":"I Had the Blues But I Shook Them Loose","authors":["Bombay Bicycle Club"],"type":"ALBUM"},
{"title":"So Long, See You Tomorrow","authors":["Bombay Bicycle Club"],"type":"ALBUM"},
{"title":"The Bicycle - Towards a Global History","authors":["P. Smethurst"],"type":"BOOK"},
{"title":"The Everything Bicycle Book","authors":["Roni Sarig"],"type":"BOOK"}]
```

#### Simple health check
```bash
curl -v 'http://localhost:8080/health'
```
possible response:
```
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: text/plain;charset=UTF-8

No webservice is unhealthy. Ready to go
```

#### Live information about a webservice

This is a stream of events in the form of a websocket connection

_please note that for the current implementation a search request has to be done first_

<http://localhost:8080/monitor/stats/books/stream>
<http://localhost:8080/monitor/stats/albums/stream>

#### Aggregated information about a specific webservice

```bash
curl 'http://localhost:8080/monitor/stats/books'
```

possible response:
```json
{"healthy":false,"windowSize":5.000000000,"nrOfRequests":9,"nrOfErrors":1,"errorRate":11.11111111111111,"requestsPerSecond":1.8,"minResponseTime":312,"maxResponseTime":507,"avgResponseTime":381}
```

## Notes
My investigation let me to find a defect in release 5.0RC3 of WebFlux / Spring reactive.
My pull request to fix this was accepted: [Jackson encoder and decoder should use provided mime types #1499](https://github.com/spring-projects/spring-framework/pull/1499)

ab (Apache Bench) hangs on the end-points I created. 
It seems to be caused by the fact that `transfer-encoding: chunked` is used by the server.
I haven't investigated if this can be changed to `content-length`.
An alternative approach is to cURL in a loop:
```bash
for ((i=1;i<=50;i++)); do curl 'http://localhost:8080/search/albumsAndBooks/Bicycle'; done
``` 
