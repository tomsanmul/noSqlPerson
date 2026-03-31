# AWS SDK v2 Enhanced Client: Java

## Summary

> The **AWS SDK for Java 2.x Enhanced Client** is the modern, official way to work with DynamoDB in Java. It lets you:

- <mark>Map your Java objects</mark> (like a `Person` class) directly to `DynamoDB` table items
- Save, read, update, and delete data using simple methods
- Avoid writing lots of low-level code
- Keep <mark>full control</mark> over how everything works

## Step by step

### Local AWS Credentials Setup (Using AWS CLI)

Install<mark> LInux AWS  CLI </mark> (Command Line Interface) on Ubuntu, follow these steps:

**Update the Package Index**

Run the following command to ensure your package list is up-to-date:  

```rust
sudo apt update
```

**Install Dependencies**

Ensure you have the necessary dependencies installed:  

```rust
sudo apt install -y unzip curl
```

**Download the AWS CLI Installer**

Use `curl` to download the AWS CLI v2 installer:  

```rust
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
```

**Extract the Installer**

Unzip the downloaded file:  

```rust
unzip awscliv2.zip
```

**Run the Installer**

Run the installation script:  

```rust
sudo ./aws/install
```

**Verify the Installation**

Confirm that the AWS CLI is installed and check its version:  

```rust
aws --version
```

You should see output similar to:  

```rust
aws-cli/2.x.x Python/x.x.x Linux/x.x.x
```

**Clean Up**

(Optional) Remove the downloaded files to free up space:  

```rust
rm -rf awscliv2.zip aws
```

**Configure the AWS CLI**

Set up your AWS credentials:  

```rust
aws configure
```

You’ll need to provide:

- **Access Key ID** / **Secret Access Key**
- **Default region** (e.g., `eu-central-1`)
- **Output format** (e.g., `json`, `text`, or `table`)

It will ask for your AWS Access Key ID, Secret Access Key, default region (e.g. <mark>eu-central-1</mark>), and output format (<mark>json</mark>).
These keys are saved safely in a hidden folder on your computer (`~/.aws/credentials`). <mark>Spring Cloud AWS</mark> will automatically read them. 

> **Warning**: For better security, avoid using your root account keys in daily development — prefer IAM user keys instead. 

The output will prompt for the profile-specific settings:

```rust
AWS Access Key ID [None]: YOUR_ACCESS_KEY_ID
AWS Secret Access Key [None]: YOUR_SECRET_ACCESS_KEY
Default region name [None]: YOUR_PREFERRED_REGION
Default output format [None]: json
```

### Step 1: Add the Required Dependencies

Open your `pom.xml` file and add these dependencies (Spring Boot will manage the versions automatically):

```xml
<dependencies>
    <!-- AWS SDK for DynamoDB -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb</artifactId>
        <version>2.42.23</version>
    </dependency>

    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb-enhanced</artifactId>
        <version>2.42.23</version>
    </dependency>

    <!-- Helps Spring Boot handle AWS credentials and region automatically
    <dependency>
        <groupId>io.awspring.cloud</groupId>
        <artifactId>spring-cloud-aws-starter</artifactId>
    </dependency> -->

</dependencies>
```

#### Broken dependency

> The <mark>spring-cloud-aws-starter</mark> `dependency` (from `io.awspring.cloud)` is the root cause of an issue. It brings in a lot of **auto-configuration** classes and can interfere with Spring Boot's default component scanning behavior, especially in certain project structures or when version mismatches exist with your Spring Boot version and the AWS SDK v2 dependencies you're already using (dynamodb + dynamodb-enhanced 2.42.23).

Why this happens ?

- @SpringBootApplication implicitly includes `@ComponentScan` that starts scanning from the **package of your main class** and all sub-packages.
- Some versions/configurations of <mark>Spring Cloud AWS</mark> (particularly older 2.x or mismatched 3.x) pull in meta-data, auto-config imports, or context setup that can shift effective scanning roots, cause classpath conflicts, or make Spring treat the main class differently during bootstrap.
- This often manifests as your main class (or other @Component/@Service/@Repository/@Controller classes) not being picked up, or the application failing to start with strange "class not found as bean" behavior.

### Step 2: Create the Person Class

> **@DynamoDbBean** is a class-level annotation in the **AWS SDK for Java 2.x** (Enhanced DynamoDB client). 

> It marks a POJO as a mappable DynamoDB entity, enabling automatic schema generation via `BeanTableSchema`. Use it with property-level annotations like `@DynamoDbPartitionKey`, `@DynamoDbAttribute`, etc., for seamless object-to-table mapping. 

> For immutable classes, prefer `@DynamoDbImmutable` instead.

Create a simple `Java class` for your data. This class tells <mark>DynamoDB</mark> how to store the information.

```java
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@DynamoDbBean   // This tells the Enhanced Client this class can be mapped to DynamoDB
public class Person {

    private String id;           // Partition Key (unique ID for the person)
    private String operation;    // Sort Key (e.g. "CREATE", "UPDATE", "VIEW", "DELETE")
    private String name;
    private int age;
    private String email;
    private Instant createdAt;   // When the person was added

    // Partition Key (required)
    @DynamoDbPartitionKey
    @DynamoDbAttribute("personId")   // Optional: custom name in DynamoDB
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Sort Key - added for composite primary key (personId + operation)
    @DynamoDbSortKey
    @DynamoDbAttribute("operation")  // Optional: custom name in DynamoDB
    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    // Normal getters and setters for the other fields
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

    @Override
    public String toString() {
        return "Person{" +
                "id='" + id + '\'' +
                ", operation='" + operation + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
```

**Tip**: Always use the object version (`Integer`, not `int`) if you want to allow `null` values. For simple cases like this, primitives are fine.

### Step 3: Set Up the Enhanced Client

Create a configuration class so Spring can create the DynamoDB clients for you.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder().build();   // Spring Cloud AWS helps with credentials
    }

    @Bean
    public DynamoDbEnhancedClient enhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
```

This **Spring Boot Configuration** class sets up AWS DynamoDB beans for dependency injection.

- `@Configuration` marks this class as a source of Spring beans.
- The first `@Bean` creates a standard `DynamoDbClient` (low-level client). <mark>Spring Cloud AWS</mark> automatically handles credentials and region configuration.
- The second `@Bean` creates `DynamoDbEnhancedClient`, which is the high-level <mark>Enhanced DynamoDB client</mark>. It wraps the low-level client and enables easy mapping of Java objects using annotations like `@DynamoDbBean`.

This setup allows you to inject `DynamoDbEnhancedClient` anywhere in your application.

### Step 4: Save a Person

Here’s how you actually save data. Create a service class:

```java
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.UUID;

@Service
public class PersonService {

    private final DynamoDbTable<Person> personTable;

    // Spring automatically injects the Enhanced Client
    public PersonService(DynamoDbEnhancedClient enhancedClient) {
        // Connect the Person class to a 
        //DynamoDB table called "person"
        //with schema Person.class
        this.personTable = 
                enhancedClient.table(
                    "person",
                    TableSchema.fromBean(Person.class)
                );
    }

    /**
     * Saves a Person to DynamoDB.
     * If the person already exists (same ID), it will be replaced.
     */
    public Person save(Person person) {

        // Give a unique ID if none exists
        if (person.getId() == null || person.getId().isEmpty()) {
            person.setId(UUID.randomUUID().toString());
        }

        // Add the current time
        person.setCreatedAt(Instant.now());

        // Save to DynamoDB (this is the main line!)
        personTable.putItem(person);

        return person;   // Return the saved person
    }
}
```

*How to Use It*

In a `controller` or anywhere else, you can call:

```java
personService.save(myPersonObject);
```

> That’s it! The Enhanced Client handles converting your Java object into DynamoDB format automatically.

### Quick Tips

- **Table Name**: Change `"PersonTable"` to whatever you want. Create this table in the AWS Console or with code first.
- **Partition Key**: Every table needs one (here it’s `id`). It must be unique.
- **ID Generation**: Using `UUID` is a simple and safe way for beginners.
- **Error Handling**: In real apps, wrap the `putItem()` call in a try-catch for `DynamoDbException`.
- **Testing Locally**: You can run DynamoDB Local for development (no AWS needed).

## Complete service

Here's the **complete and clean `PersonService`** with all basic **CRUD operations**.

It works with the `Person` class that has:

- **Partition Key**: `id` (personId)
- **Sort Key**: `operation`

```java
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PersonService {

    private final DynamoDbTable<Person> personTable;

    public PersonService(DynamoDbEnhancedClient enhancedClient) {
        // Connect the Person class to your DynamoDB table
        this.personTable = enhancedClient.table("person",
                TableSchema.fromBean(Person.class));
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

    /**
     * Find one specific Person by id and operation (exact match)
     */
    public Optional<Person> findByIdAndOperation(String id, String operation) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(operation)
                .build();

        Person person = personTable.getItem(key);
        return Optional.ofNullable(person);
    }

    /**
     * Find all operations for a specific person (using only Partition Key)
     */
    public List<Person> findAllById(String id) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                k -> k.partitionValue(id)
        );

        PageIterable<Person> results = personTable.query(
                QueryEnhancedRequest.builder()
                        .queryConditional(queryConditional)
                        .build()
        );

        // Convert the results into a simple List
        return results.stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    /**
     * Update an existing Person.
     * This is the same as save(), but we keep the method name for clarity.
     */
    public Person update(Person person) {
        // Make sure both keys are present before updating
        if (person.getId() == null || person.getOperation() == null) {
            throw new IllegalArgumentException("Both id and operation are required to update");
        }
        return save(person);   // Reuse save() - DynamoDB uses upsert
    }

    /**
     * Delete a specific Person by id and operation
     */
    public void deleteByIdAndOperation(String id, String operation) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(operation)
                .build();

        personTable.deleteItem(key);
    }

    /**
     * Delete all records for a specific person (all operations)
     */
    public void deleteAllById(String id) {
        List<Person> allRecords = findAllById(id);

        for (Person p : allRecords) {
            deleteByIdAndOperation(p.getId(), p.getOperation());
        }
    }
}
```

### How to Use the Service

```java
// 1. Save a new person
Person person = new Person();
person.setName("John Doe");
person.setAge(30);
person.setEmail("john@example.com");
person.setOperation("CREATE");        // or "UPDATE", "LOGIN", etc.

Person saved = personService.save(person);

// 2. Find one specific record
Optional<Person> found = personService.findByIdAndOperation(saved.getId(), "CREATE");

// 3. Find all operations for this person
List<Person> allForPerson = personService.findAllById(saved.getId());

// 4. Delete one record
personService.deleteByIdAndOperation(saved.getId(), "CREATE");
```

**Important Notes**

- The table **must** have `personId` as <mark>Partition Key</mark> and `operation` as <mark>Sort Key</mark>.
- `save()` and `update()` do the same thing in DynamoDB (it replaces the item if it exists).
- Always provide both `id` and `operation` when you want to `get`, `update`, or `delete` a **specific** record.
- Use `findAllById()` when you want to see everything related to one `person`.

## PersonController.java

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/person")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    // ==================== CREATE / UPDATE ====================

    /**
     * Create or Update a Person
     * Example: POST http://localhost:8080/api/persons
     */
    @PostMapping
    public ResponseEntity<Person> createOrUpdate(@RequestBody Person person) {
        Person savedPerson = personService.save(person);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPerson);
    }

    // ==================== READ ====================

    /**
     * Get a specific Person by id and operation
     * Example: GET http://localhost:8080/api/persons/123e4567-e89b-12d3-a456-426614174000/CREATE
     */
    @GetMapping("/{id}/{operation}")
    public ResponseEntity<Person> getByIdAndOperation(
            @PathVariable String id,
            @PathVariable String operation) {

        Optional<Person> person = personService.findByIdAndOperation(id, operation);

        return person.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all operations for a specific Person
     * Example: GET http://localhost:8080/api/persons/123e4567-e89b-12d3-a456-426614174000
     */
    @GetMapping("/{id}")
    public ResponseEntity<List<Person>> getAllById(@PathVariable String id) {
        List<Person> persons = personService.findAllById(id);
        return ResponseEntity.ok(persons);
    }

    // ==================== UPDATE ====================

    /**
     * Update a Person (same as create, but clearer name)
     * Example: PUT http://localhost:8080/api/persons
     */
    @PutMapping
    public ResponseEntity<Person> update(@RequestBody Person person) {
        Person updatedPerson = personService.update(person);
        return ResponseEntity.ok(updatedPerson);
    }

    // ==================== DELETE ====================

    /**
     * Delete a specific Person record by id and operation
     * Example: DELETE http://localhost:8080/api/persons/123e4567-e89b-12d3-a456-426614174000/CREATE
     */
    @DeleteMapping("/{id}/{operation}")
    public ResponseEntity<Void> deleteByIdAndOperation(
            @PathVariable String id,
            @PathVariable String operation) {

        personService.deleteByIdAndOperation(id, operation);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all records for a specific Person
     * Example: DELETE http://localhost:8080/api/persons/123e4567-e89b-12d3-a456-426614174000
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAllById(@PathVariable String id) {
        personService.deleteAllById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### How to Test the API

<mark>BASE URL</mark> : `/api/v1/person/`

| Operation               | HTTP Method | URL Example                                 | Description                       |
| ----------------------- | ----------- | ------------------------------------------- | --------------------------------- |
| Create / Save           | POST        | <mark>BASE URL</mark> +                     | Create a new person               |
| Get one record          | GET         | <mark>BASE URL</mark> + `{id}/{operation}`  | Get specific operation            |
| Get all for a person    | GET         | <mark>BASE URL</mark> + `{id}`              | Get all operations for one person |
| Update                  | PUT         | <mark>BASE URL</mark> +                     | Update existing person            |
| Delete one record       | DELETE      | <mark>BASE URL</mark> + ``{id}/{operation}` | Delete specific operation         |
| Delete all for a person | DELETE      | <mark>BASE URL</mark> + `{id}`              | Delete everything for one person  |

### Example JSON Body for POST / PUT

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "operation": "CREATE",
  "name": "Alice Smith",
  "age": 28,
  "email": "alice@example.com"
}
```

**Note**: `id` and `operation` are optional in the request body. The service will generate them automatically if missing.``
