# Adding GraphQL Subscriptions to a Spring Boot Project

> Step-by-step guide for the **noSqlPerson** project  
> Stack: Spring Boot 4.0.5 · Spring for GraphQL · DynamoDB · Java 21

<mark>SUBSCRIPTION_GUIDE</mark> : with a complete step-by-step:

1. **What subscriptions are** and the architecture overview
2. **Step 1** — [pom.xml](cci:7://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/pom.xml:0:0-0:0) dependency (`spring-boot-starter-websocket`)
3. **Step 2** — [application.properties](cci:7://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/resources/application.properties:0:0-0:0) (WebSocket + SSE config)
4. **Step 3** — GraphQL schema (`type Subscription`)
5. **Step 4** — [PersonEventPublisher.java](cci:7://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/java/dev/cifo/noSqlPerson/PersonEventPublisher.java:0:0-0:0) with a <mark>Reactor concepts table</mark>
6. **Step 5** — Wiring the publisher into [PersonService.save()](cci:1://file:///home/albert/MyProjects/Sandbox/noSqlPerson/noSqlPerson/src/main/java/dev/cifo/noSqlPerson/PersonService.java:55:4-72:5)
7. **Step 6** — `@SubscriptionMapping` in the controller
8. **Testing** — GraphiQL instructions with example queries
9. **Summary** — files changed + full data flow diagram

## What is a GraphQL Subscription?

A **Subscription** is the third root operation type in GraphQL (alongside Query and Mutation).  
It lets clients **receive real-time updates** whenever something changes on the server.

```
Client ──subscribe──▶ Server
Client ◀──event 1──── Server   (a person is created)
Client ◀──event 2──── Server   (another person is created)
...stream stays open...
```

Under the hood, Spring for GraphQL uses **Project Reactor** (`Flux`) to model the
event stream and delivers it to clients over **WebSocket** or **SSE** (Server-Sent Events).

## Architecture Overview

```
┌───────────────┐  publish()   ┌───────────────────────┐  Flux<Person>  ┌────────────────┐
│ PersonService │ ───────────▶ │ PersonEventPublisher  │ ─────────────▶ │ GraphQL Client │
│   (save)      │              │ (Reactor Sink)        │  WebSocket/SSE │ (GraphiQL, app)│
└───────────────┘              └───────────────────────┘                └────────────────┘
```

Four things are needed:

1. **Schema** — declare the `Subscription` type
2. **Publisher** — a shared Reactor `Sink` that acts as an event bus
3. **Emitter** — the service method that pushes events into the sink
4. **Resolver** — a `@SubscriptionMapping` controller method that returns the `Flux`

## Step 1 — Add dependencies

`pom.xml`:

We need `spring-boot-starter-websocket` so that GraphiQL (and other clients) can
subscribe over WebSocket. The `reactor-core` library is already included transitively
by `spring-boot-starter-graphql`.

```xml
<!-- Already present -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>

<!-- ADD THIS — enables WebSocket transport for subscriptions -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## Step 2 — Enable transports

`application.properties`:

```properties
# GraphiQL playground (already enabled)
spring.graphql.graphiql.enabled=true

# WebSocket endpoint — required for GraphiQL subscriptions
spring.graphql.websocket.path=/graphql

# SSE endpoint — optional, useful for programmatic HTTP clients
spring.graphql.sse.enabled=true
```

- **WebSocket** (`ws://localhost:8080/graphql`) — used by GraphiQL and most JS clients.
- **SSE** (`POST /graphql` with `Accept: text/event-stream`) — useful for HTTP-only clients.

## Step 3 — Declare the Subscription in the GraphQL schema

File: `src/main/resources/graphql/schema.graphqls`

Add after the `Mutation` type:

```graphql
type Subscription {
  personCreated: Person!
}
```

This tells GraphQL: *"clients can subscribe to `personCreated` and will receive
a `Person` object every time the event fires."*

## Step 4 — Create the event publisher

`PersonEventPublisher.java`:

File: `src/main/java/dev/cifo/noSqlPerson/PersonEventPublisher.java`

```java
package dev.cifo.noSqlPerson;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class PersonEventPublisher {

    private final Sinks.Many<Person> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(Person person) {
        sink.tryEmitNext(person);
    }

    public Flux<Person> getStream() {
        return sink.asFlux();
    }
}
```

### Key concepts

| Concept                   | Explanation                                                                                                                       |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `Sinks.Many<Person>`      | A Reactor construct that lets you **programmatically push** items into a reactive stream. Think of it as an event bus.            |
| `.multicast()`            | Each subscriber only receives events emitted **after** they subscribe (hot stream). Multiple subscribers each get their own copy. |
| `.onBackpressureBuffer()` | If a subscriber is slow, events are buffered instead of dropped.                                                                  |
| `tryEmitNext(person)`     | Pushes one `Person` event into the sink. All current subscribers receive it.                                                      |
| `sink.asFlux()`           | Converts the sink into a `Flux<Person>` — the reactive stream that Spring for GraphQL returns to each subscriber.                 |

---

## Step 5 — Emit the event when a person is saved

`PersonService.java`:

File: `src/main/java/dev/cifo/noSqlPerson/PersonService.java`

### 5a. Inject the publisher via constructor

```java
private final DynamoDbTable<Person> personTable;
private final PersonEventPublisher eventPublisher;    // <-- NEW

public PersonService(DynamoDbEnhancedClient enhancedClient,
                     PersonEventPublisher eventPublisher) {    // <-- NEW param
    this.personTable = enhancedClient.table(
            "person",
            TableSchema.fromBean(Person.class)
    );
    this.eventPublisher = eventPublisher;    // <-- NEW
}
```

### 5b. Publish the event inside the `save()` method

```java
public Person save(Person person) {
    if (person.getId() == null || person.getId().isBlank())
        return null;
    String id = UUID.randomUUID().toString();
    person.setId(id);
    person.setCreatedAt(Instant.now());
    personTable.putItem(person);
    System.out.println("Person saved: " + getPersonByKey(id, person.getOperation()));

    eventPublisher.publish(person);    // <-- NEW: notify all subscribers

    return person;
}
```

After the person is saved to DynamoDB, we call `eventPublisher.publish(person)`.
This pushes the `Person` object into the Reactor `Sink`, which immediately
delivers it to every active subscriber.

## Step 6 — Add the subscription resolver

`PersonGraphQlController.java`:

File: `src/main/java/dev/cifo/noSqlPerson/PersonGraphQlController.java`

### 6a. Add imports

```java
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import reactor.core.publisher.Flux;
```

### 6b. Inject the publisher

```java
@Autowired
private PersonEventPublisher eventPublisher;
```

### 6c. Add the subscription method

```java
@SubscriptionMapping
public Flux<Person> personCreated() {
    return eventPublisher.getStream();
}
```

- `@SubscriptionMapping` maps this method to `type Subscription { personCreated }` in the schema.
- The method returns a `Flux<Person>` — Spring for GraphQL keeps the connection open and sends each emitted `Person` to the client as it arrives.

---

## Testing the Subscription

### 1. Start the application

```bash
./mvnw spring-boot:run
```

### 2. Open GraphiQL

Go to: [http://localhost:8080/graphiql](http://localhost:8080/graphiql)

### 3. Subscribe (Tab 1)

Paste and run:

```graphql
subscription {
  personCreated {
    id
    name
    age
    email
    operation
    createdAt
  }
}
```

GraphiQL will show a spinner — it is now listening for events over WebSocket.

### 4. Create a person (Tab 2)

Open a **new tab** in GraphiQL and run:

```graphql
mutation {
  savePerson(person: {
    operation: "CREATE"
    name: "Alice"
    age: 30
    email: "alice@example.com"
  }) {
    id
    name
  }
}
```

### 5. See the result

Switch back to **Tab 1** — the subscription tab should now display the newly
created `Person` object in real time.

---

## Summary of all files changed

| File                           | What changed                                                                         |
| ------------------------------ | ------------------------------------------------------------------------------------ |
| `pom.xml`                      | Added `spring-boot-starter-websocket` dependency                                     |
| `application.properties`       | Added `spring.graphql.websocket.path=/graphql` and `spring.graphql.sse.enabled=true` |
| `schema.graphqls`              | Added `type Subscription { personCreated: Person! }`                                 |
| `PersonEventPublisher.java`    | **New file** — Reactor Sink that publishes and streams Person events                 |
| `PersonService.java`           | Injected publisher; calls `eventPublisher.publish(person)` after save                |
| `PersonGraphQlController.java` | Added `@SubscriptionMapping personCreated()` returning `Flux<Person>`                |

---

## How it all connects (data flow)

```
1. Client sends:          subscription { personCreated { ... } }
2. Spring calls:          PersonGraphQlController.personCreated()
3. Controller returns:    eventPublisher.getStream()  →  Flux<Person>
4. Connection stays open, waiting for events...

5. Another client sends:  mutation { savePerson(...) }
6. PersonService.save()   saves to DynamoDB
7. PersonService calls:   eventPublisher.publish(person)
8. Reactor Sink emits:    person → Flux<Person>
9. Spring pushes:         person → WebSocket → subscribing client
```
