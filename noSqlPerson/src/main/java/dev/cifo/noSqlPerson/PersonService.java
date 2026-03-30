package dev.cifo.noSqlPerson;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PersonService {

    private final DynamoDbTable<Person> personTable;

    public PersonService(DynamoDbEnhancedClient enhancedClient) {
        // Connect the Person class to your DynamoDB table
        this.personTable = enhancedClient.table("person",
                TableSchema.fromBean(Person.class));
    }

    /**
     * Get all persons from the DynamoDB table.
     * @return
     */
    public PageIterable<Person> getAllPersons() {

        // ...
        PageIterable<Person> people = personTable.scan();

        return people;
    }

    /**
     * Save or update a Person.
     * If a person with the same id + operation already exists, it will be replaced.
     */
    public Person save(Person person) {

        if (person.getId() == null || person.getId().isBlank()) {
            person.setId(UUID.randomUUID().toString());
        }

        if (person.getOperation() == null || person.getOperation().isBlank()) {
            person.setOperation("CREATE");   // Default operation if none provided
        }

        person.setCreatedAt(Instant.now());

        personTable.putItem(person);   // Save to DynamoDB

        return person;
    }
}
