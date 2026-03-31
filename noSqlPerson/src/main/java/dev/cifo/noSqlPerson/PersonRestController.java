package dev.cifo.noSqlPerson;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/person")
public class PersonRestController {

    @Autowired
    private PersonService personService;

    @GetMapping("/getAll")
    public List<Person> getAll() {
        PageIterable<Person> people = personService.getAllPersons();
        // Convert PageIterable to List using Java Streams
        // stream() returns a Stream of Page objects
        // flatMap() returns a Stream of Person
        // page -> page.items().stream() : maps each Page to a Stream of Person
        // collect() returns a List of Person objects
        List<Person> personList = people.stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
        System.out.println("People count: " + personList.size());
        System.out.println("People: " + personList.toString());
        return personList;
    }

   /* @GetMapping("/page")
    public Page<Person> getPersonPage(@RequestParam(defaultValue = "10") int size) {
        return personService.getPersonPage(size);
    }

    @GetMapping("/pages")
    public PageIterable<Person> getPersonPages(@RequestParam(defaultValue = "10") int size) {
        return personService.getAllPersons();
    }*/

    @PostMapping("/save")
    public Person savePerson(@RequestBody Person person) {
        System.out.println("Saving person: " + person.toString());
        return personService.save(person);
    }

    @PostMapping("/update")
    public Person savePersonByKey(@RequestBody Person person) {
        System.out.println("Updating person: " + person.toString());
        return personService.save(person);
    }

    @GetMapping("/getByKey")
    public Person getPersonByKey(@RequestParam String id, @RequestParam String operation) {
        return personService.getPersonByKey(id, operation);
    }

    @DeleteMapping("/deleteByKey")
    public Person deletePersonByKey(@RequestParam String id, @RequestParam String operation) {
        return personService.deletePersonByKey(id, operation);
    }


}
