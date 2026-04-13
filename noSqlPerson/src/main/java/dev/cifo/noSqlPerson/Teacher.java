package dev.cifo.noSqlPerson;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean   // This tells the Enhanced Client this class can be mapped to DynamoDB
public class Teacher {

    private String id;           // Partition Key (unique ID for the person)
    private String operation;    // Sort Key (e.g. "CREATE", "UPDATE", "VIEW", "DELETE")
    private String name;
    private String department;

    // Default constructor (required for the Enhanced Client)
    // This is used by the Enhanced Client to create new instances of Person
    // It is not used by the application code
    public Teacher() {
    }

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


    @Override
    public String toString() {
        return "Teacher{" +
                "id='" + id + '\'' +
                ", operation='" + operation + '\'' +
                ", name='" + name + '\'' +
                ", email='" + department + '\'' +
                '}' + "\n";
    }
}