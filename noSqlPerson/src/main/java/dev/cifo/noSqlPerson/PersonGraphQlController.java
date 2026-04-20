package dev.cifo.noSqlPerson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.stream.StreamSupport;

@Controller
public class PersonGraphQlController {

    @Autowired
    private PersonService personService;

    @Autowired
    private PersonEventPublisher eventPublisher;

    @QueryMapping
    public List<Person> allPersons() {
        PageIterable<Person> pages = personService.getAllPersons();
        return StreamSupport.stream(pages.spliterator(), false)
                .flatMap(page -> page.items().stream())
                .toList();
    }

    @QueryMapping
    public Person personByKey(@Argument String id, @Argument String operation) {
        return personService.getPersonByKey(id, operation);
    }

    @MutationMapping
    public Person savePerson(@Argument Person person) {
        System.out.println("Saving person: " + person.toString());
        Person personSaved = personService.save(person);
        System.out.println("Person saved: " + personSaved.toString());
        return personSaved;
    }

    @MutationMapping
    public Person deletePersonByKey(@Argument String id, @Argument String operation) {
        return personService.deletePersonByKey(id, operation);
    }

   @MutationMapping
    public Person updatePerson(@Argument Person person) {
        return personService.updated(person);
    }

    @SubscriptionMapping
    public Flux<Person> personCreated() {
        return eventPublisher.getStream();
    }

    @SubscriptionMapping
    public Flux<Person> personUpdatedByAge() {
        return eventPublisher.getStream();
    }

}
