# Map approach

## Summary

- [noSQL person gsheet](https://docs.google.com/spreadsheets/d/1UYVKKlaXtwYl7nI-MfBhrRZpycEDlwbtR0srz7UiSLg/edit?gid=0#gid=0)

<mark>DynamoDB</mark> is a **NoSQL database** that is **schemaless** by design. This means:

- <mark>Every item in the same table</mark> can have **different fields** (attributes).
- You don't need to define all possible fields <mark>in advance.</mark>
- You can add new fields<mark> anytime without changing the table structure</mark> or existing data.

The `Map<String, Object>` (stored as `extraAttributes`) takes full advantage of this flexibility. Here's why it's extremely useful:

1. **Handles Changing Requirements Easily**  
   Your `Person` can start simple (Version 1 with just name, age, email). Later, when you need phone, address, preferences, lastLogin, or even completely new fields like "favoriteColor", "subscriptionPlan", or "userSettings", you don't need to modify the `Person` Java class or redeploy your code every time.  
   Just add them dynamically using `addExtraField("phone", "+1-555-1234")`.

2. **Future-Proof and Flexible**  
   Requirements almost always change over time. With a fixed class (many private fields + getters/setters), every new field forces you to:
   
   - Edit the class
   - Recompile and redeploy
   - Handle backward compatibility for old items  
   
   > With a Map, you can store **any extra data** without touching the core `Person` class. This is perfect when you have "Version 1 vs Version 2 vs Version 3..." scenarios.

3. **Saves Storage Space**  
   Only the fields you actually put in the map are saved in DynamoDB. Null or missing fields are **not stored** at all. This keeps your items clean and efficient.

4. **Simple Code for Dynamic Data**  
   You can store strings, numbers, booleans, dates, lists, or even nested maps — all in one place. The <mark>AWS Enhanced Client</mark> automatically converts the Java `Map` into a DynamoDB **Map** type.

### Small Trade-off

You lose a bit of type safety and compile-time checking for the dynamic fields (you get `Object`, so you may need to cast when reading).  
But for most real applications, the **huge gain in flexibility** is worth it.

## Updated Person Class with Dynamic Map

```java
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@DynamoDbBean
public class Person {

    private String id;                    // Partition Key
    private String operation;             // Sort Key
    private String name;
    private Integer age;
    private String email;
    private Instant createdAt;

    // Dynamic extra fields (very flexible!)
    private Map<String, Object> extraAttributes = new HashMap<>();

    // No-arg constructor (required)
    public Person() {
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("personId")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("operation")
    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // ==================== Dynamic Map ====================
    @DynamoDbAttribute("extraAttributes")
    public Map<String, Object> getExtraAttributes() {
        return extraAttributes;
    }

    public void setExtraAttributes(Map<String, Object> extraAttributes) {
        this.extraAttributes = extraAttributes != null ? extraAttributes : new HashMap<>();
    }

    // Helper method to easily add dynamic fields
    public void addExtraField(String key, Object value) {
        this.extraAttributes.put(key, value);
    }
}
```

## How to Use It

Update your `PersonService.save()` method (no big change needed):

```java
public Person save(Person person) {
    if (person.getId() == null || person.getId().isBlank()) {
        person.setId(UUID.randomUUID().toString());
    }
    if (person.getOperation() == null || person.getOperation().isBlank()) {
        person.setOperation("CREATE");
    }
    person.setCreatedAt(Instant.now());

    personTable.putItem(person);   // Map is automatically saved as a DynamoDB Map
    return person;
}
```

### Example 1: Simple Person

```java
Person simple = new Person();
simple.setName("John Doe");
simple.setAge(30);
simple.setEmail("john@example.com");
simple.setOperation("PROFILE_V1");

personService.save(simple);
```

### Example 2: Person with Dynamic Extra Fields

```java
Person extended = new Person();
extended.setName("Alice Smith");
extended.setAge(28);
extended.setEmail("alice@example.com");
extended.setOperation("PROFILE_V2");

// Add any fields dynamically
extended.addExtraField("phone", "+1-555-9876");
extended.addExtraField("address", "123 Main Street, NY");
extended.addExtraField("lastLogin", Instant.now());
extended.addExtraField("isPremium", true);
extended.addExtraField("tags", List.of("developer", "gamer"));
extended.addExtraField("settings", Map.of("theme", "dark", "notifications", true));

personService.save(extended);
```

## What Happens in DynamoDB?

- Fixed fields (`name`, `age`, `email`, etc.) are saved normally.
- The `extraAttributes` field is saved as a **DynamoDB Map** type.
- You can have completely different keys in different items — no schema change needed.
- DynamoDB only stores what you put in the map (no null waste).

### Important Tips

- Use `Map<String, Object>` — the Enhanced Client supports it well.
- Primitive wrappers (`Integer`, `Boolean`, etc.) and common types (`String`, `Instant`, `List`, nested `Map`) work automatically.
- For very complex nested objects, you may need a custom converter (advanced topic).
- When reading back the item, `getExtraAttributes()` will return the full map.

## Reading Dynamic Fields

```java
Optional<Person> found = personService.findByIdAndOperation(id, operation);

found.ifPresent(p -> {
    System.out.println("Phone: " + p.getExtraAttributes().get("phone"));
    System.out.println("Is Premium: " + p.getExtraAttributes().get("isPremium"));
});
```

This is the **cleanest and most flexible** way to handle dynamic fields while keeping your main `Person` class simple.

### Safety when reading

When reading from the map:

- The value can be any type (`String`, `Integer`, `Boolean`, `Instant`, `List`, nested `Map`, etc.).
- The key might not exist → you get `null`.
- You must **check the type** before using the value to avoid `ClassCastException`.

### Recommended Helper Methods

Add these **safe helper methods** to your `Person` class:

```java
@DynamoDbBean
public class Person {

    // ... existing fields and getters/setters ...

    private Map<String, Object> extraAttributes = new HashMap<>();

    @DynamoDbAttribute("extraAttributes")
    public Map<String, Object> getExtraAttributes() {
        return extraAttributes;
    }

    public void setExtraAttributes(Map<String, Object> extraAttributes) {
        this.extraAttributes = extraAttributes != null ? extraAttributes : new HashMap<>();
    }

    public void addExtraField(String key, Object value) {
        this.extraAttributes.put(key, value);
    }

    // ====================== SAFE READING METHODS ======================

    /**
     * Safely get a String value from extraAttributes
     */
    public Optional<String> getExtraString(String key) {
        Object value = extraAttributes.get(key);
        return (value instanceof String) ? Optional.of((String) value) : Optional.empty();
    }

    /**
     * Safely get an Integer value
     */
    public Optional<Integer> getExtraInteger(String key) {
        Object value = extraAttributes.get(key);
        if (value instanceof Integer) {
            return Optional.of((Integer) value);
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).intValue());
        }
        return Optional.empty();
    }

    /**
     * Safely get a Boolean value
     */
    public Optional<Boolean> getExtraBoolean(String key) {
        Object value = extraAttributes.get(key);
        return (value instanceof Boolean) ? Optional.of((Boolean) value) : Optional.empty();
    }

    /**
     * Safely get an Instant (date/time) value
     */
    public Optional<Instant> getExtraInstant(String key) {
        Object value = extraAttributes.get(key);
        return (value instanceof Instant) ? Optional.of((Instant) value) : Optional.empty();
    }

    /**
     * Safely get any value as String (useful for logging or display)
     */
    public String getExtraAsString(String key) {
        Object value = extraAttributes.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Check if a dynamic field exists
     */
    public boolean hasExtraField(String key) {
        return extraAttributes.containsKey(key);
    }
}
```

### How to Use These Methods

In your **Service**, **Controller**, or any other class:

```java
Optional<Person> optionalPerson = personService.findByIdAndOperation(id, operation);

if (optionalPerson.isPresent()) {
    Person p = optionalPerson.get();

    // Safe reading examples
    String phone = p.getExtraString("phone").orElse("Not provided");
    Integer age = p.getExtraInteger("age").orElse(0);
    Boolean isPremium = p.getExtraBoolean("isPremium").orElse(false);
    Instant lastLogin = p.getExtraInstant("lastLogin").orElse(null);

    System.out.println("Phone: " + phone);
    System.out.println("Is Premium: " + isPremium);

    // If you just want to display whatever is there
    String settings = p.getExtraAsString("settings");
}
```

### In a REST Controller (Returning Dynamic Data)

You can expose the whole map safely like this:

```java
@GetMapping("/{id}/{operation}")
public ResponseEntity<Map<String, Object>> getPersonWithExtras(
        @PathVariable String id,
        @PathVariable String operation) {

    Optional<Person> opt = personService.findByIdAndOperation(id, operation);

    if (opt.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    Person person = opt.get();

    // Build a response map that combines fixed + dynamic fields
    Map<String, Object> response = new HashMap<>();
    response.put("id", person.getId());
    response.put("operation", person.getOperation());
    response.put("name", person.getName());
    response.put("email", person.getEmail());
    response.putAll(person.getExtraAttributes());   // Add all dynamic fields

    return ResponseEntity.ok(response);
}
```

### Best Practices Summary

- Always use `Optional` or provide a default value (`orElse(...)`).
- Prefer specific methods like `getExtraString()`, `getExtraInteger()` over raw casting.
- Use `hasExtraField(key)` before accessing if needed.
- For very complex nested structures, consider the **Enhanced Document API** later.

> This approach keeps your code safe, readable, and prevents runtime crashes when fields are missing or have unexpected types.
