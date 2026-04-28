package dev.cifo.noSqlPerson;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PersonService {

    // why private final?
    // final means that the variable cannot be changed after it is initialized
    // private means that the variable cannot be accessed from outside the class
    private final DynamoDbTable<Person> personTable;
    private final PersonEventPublisher eventPublisher;
    // dependency injection because it is a dependency of the class
    public PersonService(DynamoDbEnhancedClient enhancedClient, PersonEventPublisher eventPublisher) {
        // Connect the Person class to your DynamoDB table
        // Person.class is the class that will be used to map the table
        // with items from the table to Person objects, that is DynamoDB beans
        this.personTable = enhancedClient.table(
                "courses",
                TableSchema.fromBean(Person.class)
        );
        this.eventPublisher = eventPublisher;
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

        // publish the event, that is the person that was saved
        eventPublisher.publish(person);

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
        person.setUpdatedAt(Instant.now());
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

    /**
     * delete a person by their composite key (id + operation).
     * @param id The partition key
     * @param operation The sort key
     * @return The person if found, null otherwise
     */
    public Person deletePersonByKey(String id, String operation){

        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(operation)
                .build();

        Person deletedPerson = personTable.deleteItem(key);
        System.out.println("Person deleted: " + deletedPerson.toString());
        return deletedPerson;
    }


    // Filter persons by age using a scan and a filter expression
    // with enhanced client ScanEnhancedRequest
    // Expression is used to create a filter expression
    // ScanEnhancedRequest is used to create a scan request
    // AttributeValue is used to create a value for the filter expression
    // Alternative we can use PartiQL to create a filter expression
    // PartiQL is a SQL-like query language for DynamoDB
    // https://partiql.org/
    public PageIterable<Person> filterPersonsByAge(int age){
        Expression filterExpression = Expression.builder()
                .expression("age = :ageVal")
                .putExpressionValue(":ageVal", AttributeValue.builder().n(String.valueOf(age)).build())
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        // Uses the low-level DynamoDbClient (not the Enhanced Client)
        // This is not recommended because it is not type-safe
        // and maybe it is not as easy to use
        /*ExecuteStatementRequest request = ExecuteStatementRequest.builder()
                .statement("SELECT * FROM person WHERE age = ?")
                .parameters(AttributeValue.builder().n(String.valueOf(age)).build())
                .build();

        ExecuteStatementResponse response = dynamoDbClient.executeStatement(request);*/


        return personTable.scan(scanRequest);
    }

    // Query persons by id using a query and a query conditional
    // with enhanced client QueryConditional
    // Key is used to create a key for the query
    public SdkIterable<Page<Person>> queryById(String id) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(id).build()
        );
        return personTable.query(queryConditional);
    }

    // Query persons by id and operation prefix using a query and a query conditional
    // with enhanced client QueryConditional
    // Key is used to create a key for the query
    public SdkIterable<Page<Person>> queryByIdAndOperationPrefix(String id, String operationPrefix) {
        QueryConditional queryConditional = QueryConditional.sortBeginsWith(
                Key.builder().partitionValue(id).sortValue(operationPrefix).build()
        );
        return personTable.query(queryConditional);
    }

    // Query persons by id and operation range using a query and a query conditional
    // with enhanced client QueryConditional
    // Key is used to create a key for the query
    public SdkIterable<Page<Person>> queryByIdAndOperationRange(String id, String from, String to) {
        QueryConditional queryConditional = QueryConditional.sortBetween(
                Key.builder().partitionValue(id).sortValue(from).build(),
                Key.builder().partitionValue(id).sortValue(to).build()
        );
        return personTable.query(queryConditional);
    }



}
