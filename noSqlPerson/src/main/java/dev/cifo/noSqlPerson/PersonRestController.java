package dev.cifo.noSqlPerson;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;

@RestController
@RequestMapping("/api/v1/person")
public class PersonRestController {

    @Autowired
    private PersonService personService;

    @GetMapping("/getAll")
    public PageIterable<Person> getAll() {
        PageIterable<Person> people = personService.getAllPersons();
        System.out.println("People: " + people.iterator().next().toString());
        return personService.getAllPersons();
    }

    @PostMapping("/save")
    public Person savePerson(@RequestBody Person person) {
        return personService.save(person);
    }
}
