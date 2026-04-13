package dev.cifo.noSqlPerson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.stream.StreamSupport;

@Controller
public class PersonGraphQlController {

    @Autowired
    private PersonService personService;

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

}
