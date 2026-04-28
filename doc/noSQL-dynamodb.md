### DynamoDB noSQL partition-doc structure

```js
📦 SINGLE TABLE "Courses"
├── 🟦 BASE TABLE (PK: courseId, SK: courseItem)
│   ├── Partition courseId=3
│   │   └── Item #1: {
│   │       courseId: 3, courseItem: "DETAILS", 
│   │       name: "web app", hours: 500, type: "computer science",
│   │       schoolId: 7, schoolItem: "DETAILS"
│   │   }
│   │
│   └── Partition courseId=4
│       └── Item #2: {
│           courseId: 4, courseItem: "DETAILS", 
│           name: "mobile app", hours: 300, type: "computer science",
│           schoolId: 7, schoolItem: "DETAILS"
│       }
│
└── 🟨 GSI "SchoolIndex" (PK: schoolId, SK: schoolItem)
    └── Partition schoolId=7  (both items land here!)
        ├── Projected Item #1: {
        │   schoolId: 7, schoolItem: "DETAILS", courseId: 3,
        │   name: "web app", hours: 500, type: "computer science"
        │   }
        │
        └── Projected Item #2: {
            schoolId: 7, schoolItem: "DETAILS", courseId: 4,
            name: "mobile app", hours: 300, type: "computer science"
        }
```

### GSI attribute age

```java
@DynamoDbSecondaryPartitionKey(indexNames = "age-index")
public int getAge() {
    return age;
}
```

```java
@DynamoDbSecondarySortKey(indexNames = "age-index")
public String getName() {
    return name;
}
```

### Then query it in PersonService

```java
DynamoDbIndex<Person> ageIndex = personTable.index("age-index");

QueryConditional query = QueryConditional.keyEqualTo(
        Key.builder().partitionValue(age).build()
);

SdkIterable<Page<Person>> results = ageIndex.query(query);
```

New import needed:

```java
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
```

### AWS CLI

```bash
aws dynamodb update-table \
  --table-name person \
  --attribute-definitions AttributeName=age,AttributeType=N \
  --global-secondary-index-updates '[{
    "Create": {
      "IndexName": "age-index",
      "KeySchema": [{"AttributeName":"age","KeyType":"HASH"}],
      "Projection": {"ProjectionType":"ALL"}
    }
  }]'
```

### Summary

| Where                                                                                                                                                      | What                                                                                                                                                                                                      |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Person.java](cci:7://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/java/dev/cifo/noSqlPerson/Person.java:0:0-0:0)               | `@DynamoDbSecondaryPartitionKey(indexNames = "age-index")` on [getAge()](cci:1://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/java/dev/cifo/noSqlPerson/Person.java:62:4-64:5) |
| [PersonService.java](cci:7://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/java/dev/cifo/noSqlPerson/PersonService.java:0:0-0:0) | `personTable.index("age-index").query(...)`                                                                                                                                                               |
| **Create GSI**                                                                                                                                             | Create the GSI on the `person` table: AWS Console → person table → Indexes → Create index                                                                                                                 |
| **AWS (CLI)**                                                                                                                                              | `aws dynamodb update-table \`                                                                                                                                                                             |
