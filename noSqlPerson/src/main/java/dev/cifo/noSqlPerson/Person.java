package dev.cifo.noSqlPerson;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@DynamoDbBean   // This tells the Enhanced Client this class can be mapped to DynamoDB
public class Person {

    private String courseId;           // Before: id / Partition Key (unique ID for the person, COURSE)
    private String courseItem;    // Before: operation / Sort Key (e.g. "STUDENT", "TEACHER", "STAFF", "LEGAL")
    private String name;
    private int age;
    private String email;
    private Instant createdAt;   // When the person was added
    private Instant updatedAt;   // When the person was updated
    // GSI
    private String schoolId;
    private String schoolItem;

    // Dynamic extra fields (very flexible!)
    private Map<String, String> extraAttributes = new HashMap<>();

    // Default constructor (required for the Enhanced Client)
    // This is used by the Enhanced Client to create new instances of Person
    // It is not used by the application code
    public Person() {
        //this.courseId = UUID.randomUUID().toString();
    }

    /*public Person(String name, int age, String email, Instant createdAt) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.createdAt = createdAt;
    }*/

    // Partition Key (required)
    @DynamoDbPartitionKey
    @DynamoDbAttribute("courseId")   // Optional: custom name in DynamoDB
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    // Sort Key - added for composite primary key (personId + operation)
    @DynamoDbSortKey
    @DynamoDbAttribute("courseItem")  // Sort key matching DynamoDB table schema
    public String getCourseItem() {
        return courseItem;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {})
    @DynamoDbAttribute("schoolId")
    public String getSchoolId() {return schoolId ;}

    public void setSchoolId(String schoolId) {this.schoolId = schoolId;}

    @DynamoDbSecondarySortKey(indexNames = {})
    @DynamoDbAttribute("schoolItem")
    public String getSchoolItem() {return schoolItem ;}

    public void setSchoolItem(String schoolItem) {this.schoolItem = schoolItem;}


    public void setCourseItem(String courseItem) {
        this.courseItem = courseItem;
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

    // ==================== Dynamic Map ====================
    /*@DynamoDbAttribute("extraAttributes")
    public Map<String, String> getExtraAttributes() {
        return extraAttributes;
    }

    public void setExtraAttributes(Map<String, String> extraAttributes) {
        this.extraAttributes = extraAttributes != null ? extraAttributes : new HashMap<>();
    }

    // Helper method to easily add dynamic fields
    public void addExtraField(String key, String value) {
        this.extraAttributes.put(key, value);
    }*/

    @Override
    public String toString() {
        return "Person{" +
                "id='" + courseId + '\'' +
                ", operation='" + courseItem + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", schoolId='" + schoolId + '\'' +
                ", schoolItem='" + schoolItem + '\'' +
                '}' + "\n";
    }
}