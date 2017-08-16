package nl.lindooren.springreactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringReactiveApplication.class, args);
    }

// Leaving this as a reference of an alternative configuration possibility
// Used like:
// @AutoWired WebClient.Builder webclientBuilder
// webClient = webclientBuilder.baseUrl("https://itunes.apple.com").build();
//
//    @Bean
//    public CodecCustomizer jacksonLegacyJsonCustomizer(ObjectMapper mapper) {
//        return (configurer) -> {
//            MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
//            CodecConfigurer.CustomCodecs customCodecs = configurer.customCodecs();
//            customCodecs.decoder(
//                    new Jackson2JsonDecoder(mapper, textJavascript));
//            customCodecs.encoder(
//                    new Jackson2JsonEncoder(mapper, textJavascript));
//        };
//    }
}
