# Adding GraphQL + Teacher

## Important Distinction: Two Approaches

There are **two different ways** to add GraphQL here, and they're architecturally very different:

|                          | **AWS AppSync**                    | **Spring Boot GraphQL**                                                                                                                                                        |
| ------------------------ | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Where GraphQL runs**   | AWS-managed service (cloud)        | Inside your Spring Boot app                                                                                                                                                    |
| **Connects to DynamoDB** | Directly via VTL/JS resolvers      | Through your existing [PersonService](cci:2://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/java/dev/cifo/noSqlPerson/PersonService.java:14:0-104:1) |
| **Java code needed**     | Minimal (just the `Teacher` model) | Schema + `@Controller` resolvers                                                                                                                                               |
| **Deployment**           | AWS Console / CDK / CloudFormation | Just your Spring Boot app                                                                                                                                                      |

---

## Step 1: The `Teacher` Class (needed for both approaches)

There's a **DynamoDB inheritance caveat**: `@DynamoDbBean` works with subclasses, but the Enhanced Client resolves getters from the class hierarchy. You have two options for table design:

- **Single-table design** — `Teacher` and [Person](cci:2://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/java/dev/cifo/noSqlPerson/Person.java:5:0-100:1) share the `person` table, distinguished by a `type` attribute
- **Separate table** — `Teacher` gets its own `teacher` table

Here's the `Teacher` class (single-table approach):

```java
package dev.cifo.noSqlPerson;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class Teacher extends Person {

    private String subject;
    private String department;
    private String employeeId;

    public Teacher() {
        super();
    }

    @DynamoDbAttribute("subject")
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    @DynamoDbAttribute("department")
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @DynamoDbAttribute("employeeId")
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    @Override
    public String toString() {
        return "Teacher{" +
                "subject='" + subject + '\'' +
                ", department='" + department + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", " + super.toString() +
                '}';
    }
}
```

And a `TeacherService`:

```java
package dev.cifo.noSqlPerson;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import java.time.Instant;
import java.util.UUID;

@Service
public class TeacherService {

    private final DynamoDbTable<Teacher> teacherTable;

    public TeacherService(DynamoDbEnhancedClient enhancedClient) {
        this.teacherTable = enhancedClient.table(
                "teacher",
                TableSchema.fromBean(Teacher.class)
        );
    }

    public PageIterable<Teacher> getAllTeachers() {
        return teacherTable.scan();
    }

    public Teacher save(Teacher teacher) {
        if (teacher.getId() == null || teacher.getId().isBlank()) {
            teacher.setId(UUID.randomUUID().toString());
            teacher.setOperation("CREATE");
            teacher.setCreatedAt(Instant.now());
        } else {
            teacher.setOperation("UPDATE");
            teacher.setUpdatedAt(Instant.now());
        }
        teacherTable.putItem(teacher);
        return teacher;
    }

    public Teacher getTeacherByKey(String id, String operation) {
        Key key = Key.builder().partitionValue(id).sortValue(operation).build();
        return teacherTable.getItem(key);
    }

    public Teacher deleteTeacherByKey(String id, String operation) {
        Key key = Key.builder().partitionValue(id).sortValue(operation).build();
        return teacherTable.deleteItem(key);
    }
}
```

---

## Step 2a: AWS AppSync Approach

AppSync is **not a Java dependency** — it's an AWS infrastructure service. You configure it via the **AWS Console**, **CDK**, or **CloudFormation**.

### GraphQL Schema (defined in AppSync console)

```graphql
type Person {
  id: ID!
  operation: String!
  name: String
  age: Int
  email: String
  createdAt: AWSDateTime
  updatedAt: AWSDateTime
}

type Teacher {
  id: ID!
  operation: String!
  name: String
  age: Int
  email: String
  subject: String
  department: String
  employeeId: String
  createdAt: AWSDateTime
  updatedAt: AWSDateTime
}

type Query {
  getPerson(id: ID!, operation: String!): Person
  listPersons: [Person]
  getTeacher(id: ID!, operation: String!): Teacher
  listTeachers: [Teacher]
}

type Mutation {
  createTeacher(input: CreateTeacherInput!): Teacher
  updateTeacher(input: UpdateTeacherInput!): Teacher
  deleteTeacher(id: ID!, operation: String!): Teacher
}

input CreateTeacherInput {
  name: String!
  age: Int
  email: String
  subject: String
  department: String
  employeeId: String
}

input UpdateTeacherInput {
  id: ID!
  name: String
  age: Int
  email: String
  subject: String
  department: String
  employeeId: String
}
```

### AppSync DynamoDB Resolver (for `getTeacher`)

In the AppSync console, attach a **DynamoDB data source** pointing to your `teacher` table, then create a resolver:

**Request mapping (VTL):**

```velocity
{
  "version": "2018-05-29",
  "operation": "GetItem",
  "key": {
    "id": $util.dynamodb.toDynamoDBJson($ctx.args.id),
    "operation": $util.dynamodb.toDynamoDBJson($ctx.args.operation)
  }
}
```

**Response mapping:**

```velocity
$util.toJson($ctx.result)
```

### Steps to set up in AWS:

1. **Create a DynamoDB table** named `teacher` (partition key: `id`, sort key: `operation`)
2. **Go to AWS AppSync Console** → Create API → Build from scratch
3. **Paste the schema** above
4. **Create Data Sources** → DynamoDB → point to `person` and `teacher` tables
5. **Attach resolvers** to each Query/Mutation field
6. Your Spring Boot app remains as the REST API; AppSync is a **separate** GraphQL endpoint

---

## Step 2b: Alternative — Spring Boot GraphQL

If you'd rather serve GraphQL **from your Spring Boot app** instead of using the managed AppSync service:

1. **Add Dependencies** (Maven):
   
   ```xml
   <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
   </dependency>
   ```

2. **Create Schema**: Place your `schema.graphqls` file in `src/main/resources/graphql/` (based on your `schema.json`).

3. **Define Data Models**:
   
   ```java
   @GraphQLType
   public class Person {
    private String id;
    private String operation;
    private Integer age;
    private String name;
    private String job;
    private String class;
    private Integer salary;
    // getters & setters
   }
   ```

4. **Create Resolver** (Controller):
   
   ```java
   @Controller
   public class PersonResolver {
   
    @QueryMapping
    public Person getPerson(@Argument String id, @Argument String operation) {
        // fetch from DB or service
        return personService.findByIdAndOperation(id, operation);
    }
   
    @QueryMapping
    public PersonConnection listPeople(@Argument TablePersonFilterInput filter, 
                                       @Argument Integer limit, 
                                       @Argument String nextToken) {
        // pagination + filtering logic
        return personService.listPeople(filter, limit, nextToken);
    }
   
    @MutationMapping
    public Person createPerson(@Argument("input") CreatePersonInput input) {
        return personService.create(input);
    }
   }
   ```

5. **Run**: Spring Boot auto-configures GraphQL endpoint at `/graphql`.

This setup supports queries, mutations, and subscriptions easily.

(Word count: 198)

Would you like a full minimal project structure or pagination example next?
