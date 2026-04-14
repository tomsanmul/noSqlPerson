package dev.cifo.noSqlPerson;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PersonService {

    // why private final?
    // final means that the variable cannot be changed after it is initialized
    // private means that the variable cannot be accessed from outside the class
    private final DynamoDbTable<Person> personTable;
    // dependency injection because it is a dependency of the class
    public PersonService(DynamoDbEnhancedClient enhancedClient) {
        // Connect the Person class to your DynamoDB table
        // Person.class is the class that will be used to map the table
        // with items from the table to Person objects, that is DynamoDB beans
        this.personTable = enhancedClient.table(
                "person",
                TableSchema.fromBean(Person.class)
        );
    }

    /**
     * Get all persons from the DynamoDB table.
     * @return
     */
    public PageIterable<Person> getAllPersons() {
        PageIterable<Person> people = personTable.scan();
        return people;
    }

    /**
     * Get a specific page of persons from DynamoDB.
     * @param pageSize The number of items per page
     * @return A single Page of Person objects
     */
    /*public Page<Person> getPersonPage(int pageSize) {
        PageIterable<Person> people = personTable.scan();
        return people.stream()
                .findFirst()
                .orElse(null);
    }*/

    /**
     * Save a Person.
     */
    public Person save(Person person) {
        // If the id is null or blank, create a new person
        if (person.getId() == null || person.getId().isBlank())
            return null;
        String id = UUID.randomUUID().toString();
        person.setId(id);
        person.setCreatedAt(Instant.now());
        // putItem will create a new item if it doesn't exist
        personTable.putItem(person); // Save to DynamoDB
        System.out.println("Person saved: " + getPersonByKey(id, person.getOperation()));


        return person;
    }

    /**
     * Update a Person.
     */
    public Person updated(Person person) {
        // If the id is null or blank, create a new person
        if (person.getId() == null || person.getId().isBlank())
            return null;
        //String id = UUID.randomUUID().toString();
        //person.setId(id);
        //person.setOperation("CREATE");
        person.setCreatedAt(Instant.now());
        // putItem will create a new item if it doesn't exist
        personTable.putItem(person); // Save to DynamoDB
        System.out.println("Person saved: " +
                getPersonByKey(person.getId(), person.getOperation()));


        return person;
    }


    /**
     * Get a person by their composite key (id + operation).
     * @param id The partition key
     * @param operation The sort key
     * @return The person if found, null otherwise
     */
    public Person getPersonByKey(String id, String operation) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(operation)
                .build();
        
        return personTable.getItem(key);
    }

    public Person deletePersonByKey(String id, String operation){

        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(operation)
                .build();

        Person deletedPerson = personTable.deleteItem(key);
        System.out.println("Person deleted: " + deletedPerson.toString());
        return deletedPerson;
    }
}
