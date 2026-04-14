# personNoSql + graphQL

### 1. Project Setup

- Use **Spring Initializr** with:
  - **Spring for GraphQL**
  - **Spring Web**
  - **Spring Data** (optional, but not needed here since you're using the AWS SDK Enhanced DynamoDB client)
- Add DynamoDB dependencies manually (see below).

### 2. Dependencies (`pom.xml` for Maven)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-graphql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- AWS SDK v2 Enhanced DynamoDB -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb-enhanced</artifactId>
        <version>2.29.0</version> <!-- or latest -->
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb</artifactId>
        <version>2.29.0</version>
    </dependency>

    <!-- Optional: for GraphiQL UI -->
    <dependency>
        <groupId>org.springframework.graphql</groupId>
        <artifactId>spring-graphql-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

For **Gradle** (`build.gradle`), replace with equivalent `implementation` lines.

### 3. `Person` Entity

with `DynamoDB` annotations:

```java
package dev.cifo.noSqlPerson;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.Map;

@DynamoDbBean
public class Person {

    private String id;                    // Partition Key
    private String operation;             // Sort Key (CREATE, UPDATE, VIEW, DELETE, ...)
    private String name;
    private int age;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> extraAttributes;

    // Default constructor (required for DynamoDB Enhanced Client)
    public Person() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSortKey
    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> getExtraAttributes() {
        return extraAttributes;
    }

    public void setExtraAttributes(Map<String, Object> extraAttributes) {
        this.extraAttributes = extraAttributes;
    }
}
```

### 4. GraphQL Schema

```graphql
type Query {
    allPersons: [Person!]!
    personByKey(id: ID!, operation: String!): Person
}

type Mutation {
    savePerson(input: PersonInput!): Person!
    deletePerson(id: ID!, operation: String!): Person
}

input PersonInput {
    id: ID
    name: String!
    age: Int!
    email: String
    extraAttributes: JSON   # Optional, you can use a Map or custom scalar
}

type Person {
    id: ID!
    operation: String!
    name: String!
    age: Int!
    email: String
    createdAt: String
    updatedAt: String
    extraAttributes: JSON
}

# Simple custom scalar for Map<String, Object>
scalar JSON
```

### 5. PersonService

Here’s a slightly cleaned-up version with better handling:

```java
package dev.cifo.noSqlPerson;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.time.Instant;
import java.util.UUID;

@Service
public class PersonService {

    private final DynamoDbTable<Person> personTable;

    public PersonService(DynamoDbEnhancedClient enhancedClient) {
        this.personTable = enhancedClient.table("person", TableSchema.fromBean(Person.class));
    }

    public PageIterable<Person> getAllPersons() {
        return personTable.scan();
    }

    public Person getPersonByKey(String id, String operation) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(operation)
                .build();
        return personTable.getItem(key);
    }

    public Person save(Person person) {
        if (person.getId() == null || person.getId().isBlank()) {
            person.setId(UUID.randomUUID().toString());
            person.setOperation("CREATE");
            person.setCreatedAt(Instant.now());
        } else {
            person.setOperation("UPDATE");
            person.setUpdatedAt(Instant.now());
        }

        personTable.putItem(person);
        return person;
    }

    public Person deletePersonByKey(String id, String operation) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(operation)
                .build();

        Person deleted = personTable.deleteItem(key);
        return deleted;
    }
}
```

### 6. GraphQL Controller / Resolver

```java
package dev.cifo.noSqlPerson;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.stream.StreamSupport;

@Controller
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

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
    public Person savePerson(@Argument PersonInput input) {
        // Convert Input → Entity
        Person person = new Person();
        person.setId(input.id());
        person.setName(input.name());
        person.setAge(input.age());
        person.setEmail(input.email());
        // extraAttributes handling can be added if needed

        return personService.save(person);
    }

    @MutationMapping
    public Person deletePerson(@Argument String id, @Argument String operation) {
        return personService.deletePersonByKey(id, operation);
    }
}
```

### 7. Input Record

recommended:

```java
package dev.cifo.noSqlPerson;

public record PersonInput(
        String id,
        String name,
        int age,
        String email,
        Object extraAttributes   // or Map<String, Object>
) {}
```

### 8. Configuration

`application.properties`:

```properties
spring.graphql.graphiql.enabled=true
# DynamoDB config (credentials, region) usually via AWS SDK defaults or application.yml
```

### How to Test

1. Run the application.
2. Open **GraphiQL** at: `http://localhost:8080/graphiql`

Example queries/mutations:

**Query all persons:**

```graphql
query {
  allPersons {
    id
    operation
    name
    age
    email
    createdAt
  }
}
```

**Save a person:**

```graphql
mutation {
  savePerson(input: {
    name: "John Doe"
    age: 30
    email: "john@example.com"
  }) {
    id
    operation
    name
    createdAt
  }
}
```

**Get by key:**

```graphql
query {
  personByKey(id: "some-uuid", operation: "CREATE") {
    name
    age
  }
}
```
