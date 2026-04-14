package dev.cifo.noSqlPerson;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class PersonEventPublisher {

    // Sinks is a reactive stream of Person objects
    // Many means that the sink can emit multiple items
    // Multicast means that the sink can be shared by multiple subscribers
    // onBackpressureBuffer means that the sink will buffer items if the subscriber is not keeping up
    private final Sinks.Many<Person> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    // publish a Person object
    // tryEmitNext is a non-blocking method that emits the next item to the sink
    public void publish(Person person) {
        sink.tryEmitNext(person);
    }

    // return a Flux of Person objects
    // Flux is a reactive stream of Person objects
    // Sinks is a reactive stream of Person objects
    public Flux<Person> getStream() {
        return sink.asFlux();
    }
}
