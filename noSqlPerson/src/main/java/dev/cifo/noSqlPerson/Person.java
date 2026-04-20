package dev.cifo.noSqlPerson;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;
import java.util.UUID;

@DynamoDbBean   // This tells the Enhanced Client this class can be mapped to DynamoDB
public class Person {

    private String id;           // Partition Key (unique ID for the person)
    private String operation;    // Sort Key (e.g. "CREATE", "UPDATE", "VIEW", "DELETE")
    private String name;
    private int age;
    private String email;
    private Instant createdAt;   // When the person was added
    private Instant updatedAt;   // When the person was updated

    // Default constructor (required for the Enhanced Client)
    // This is used by the Enhanced Client to create new instances of Person
    // It is not used by the application code
    public Person() {
        this.id = UUID.randomUUID().toString();
    }

    /*public Person(String name, int age, String email, Instant createdAt) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.createdAt = createdAt;
    }*/

    // Partition Key (required)
    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")   // Optional: custom name in DynamoDB
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

    public Instant getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Person{" +
                "id='" + id + '\'' +
                ", operation='" + operation + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}' + "\n";
    }
}