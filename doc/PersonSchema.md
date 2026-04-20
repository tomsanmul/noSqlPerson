# Person Schema

## 🎉 What is this schema?

This is a **blueprint** for a small app that manages **people** (like a simple contact list or employee list).

It tells the computer:

- What kind of data we have (a **Person**)
- What questions we can ask (the **Queries**)
- What changes we can make (the **Mutations**)
- What live updates we can receive (the **Subscription**)

Doc:

- [schema.graphqls at GitHiub](https://github.com/AlbertProfe/noSqlPerson/blob/5ec944f5c8a8244934d8f501ec4029ef0644311c/noSqlPerson/src/main/resources/graphql/schema.graphqls)

## OperatIons

### 1. The Person type (the main thing)

```graphql
type Person {
  id: ID!          ← unique identifier (like a passport number)
  operation: String!   ← this very open opton: TEACHER, STUDENT, etc
  name: String!    ← person's full name (required)
  age: Int!        ← how old they are (required)
  email: String    ← their email (optional)
  createdAt: String   ← when this person was added
  updatedAt: String   ← when this person was last changed
}
```

**Simple explanation:**
Every person has these fields.  
The `!` means **required** — you **must** give a value for that field.  
No `!` means it's optional.

The app uses `id + operation` together as a **combined key** to find a person. That's why you see `operation` everywhere.

### 2. Queries → "What can I ask for?"

```graphql
type Query {
  allPersons: [Person!]!        ← "Give me ALL the people"
  personByKey(id: ID!, operation: String!): Person   ← "Give me ONE specific person"
}
```

- **`allPersons`**  
  You can ask for the full list of people. It will return many Persons (`[Person!]`).

- **`personByKey`**  
  You want to find **one** person.  
  You need to give **two things**: their `id` AND their `operation`.  
  It's like saying: "Show me the person with id=123 and operation=HR"

### 3. Mutations → "What changes can I make?"

Mutations are for **creating**, **updating**, or **deleting** data.

```graphql
type Mutation {
  savePerson(person: person!): Person     ← Create or Save a person
  updatePerson(person: person!): Person   ← Update a person
  deletePersonByKey(id: ID!, operation: String!): Person   ← Delete a person
}
```

- **`savePerson`**  
  You send a person’s information, and it saves it (probably creates if new, or saves changes).

- **`updatePerson`**  
  Similar to save, but specifically for updating existing data.

- **`deletePersonByKey`**  
  You give `id` + `operation`, and it deletes that person.

There’s also an **input type** used for creating/updating:

```graphql
input person {
  id: ID
  operation: String!
  name: String!
  age: Int!
  email: String
}
```

This is like a **form** you fill out when you want to save or update a person.

### 4. Subscription → "Live updates!"

```graphql
type Subscription {
  personCreated: Person!     ← "Tell me when a new person is created"
}
```

<mark>This is cool! </mark> 
If someone adds a new person to the system, your app can **automatically** get notified in real time (like a live notification).

## Types

### 1. What is **type** ?

`type` is like a **blueprint** or a **template** for your data.

Think of it like this:

- You want to describe what a **Person** looks like.
- You say: “A Person has a name, age, email, etc.”

In GraphQL you write:

```graphql
type Person {
  name: String!
  age: Int!
  email: String
}
```

So `type Person` means:  
“Hey GraphQL, I’m creating a new kind of object called Person, and here are all the fields it can have.”

It’s exactly like defining a **class** or a **data structure**.

You can have many types in one schema:

- `type Person`
- `type Company`
- `type Product`
- etc.

**Super easy memory trick**:  
`type` = “This is what my data **looks like**”

### 2. What is **input** ?

`input` is also a blueprint… but it’s a **special one** used only when you **send data** to the server.

Remember the difference:

| Keyword | Used for               | Example                                    |
| ------- | ---------------------- | ------------------------------------------ |
| `type`  | Reading / getting data | `type Person` → when you ask for data      |
| `input` | Writing / sending data | `input person` → when you create or update |

Look at your schema:

```graphql
input person {
  id: ID
  operation: String!
  name: String!
  age: Int!
  email: String
}
```

This `input person` is like a **form** you fill out.

When you want to create or update a person, you send information using this input.

**Simple analogy**:

- `type Person` = how the person **appears** on the screen (the result)
- `input person` = the **form** you fill when you add or edit a person

They look similar, but they have different jobs.

### 3. What is **ID!** ?

Let’s break it into small pieces:

- **`ID`** → <mark>This is a special data type in GraphQL.</mark>  
  It is used for **unique identifiers**.  
  It’s like a passport number, employee ID, or database record number.  
  GraphQL treats `ID` as a String, but it has special meaning (it’s unique).

- **`!`** → This is the **exclamation mark** and it means **required** / **cannot be empty**.

So when you see:

```graphql
id: ID!
```

It means:

> “You **must** give me an ID. It cannot be null or missing.”

**Examples**:

- `name: String!` → Name is required
- `email: String` → Email is optional (no `!`)
- `id: ID!` → ID is required

**Simple analogy**:

- `type Person` = how the person **appears** on the screen (the result)
- `input person` = the **form** you fill when you add or edit a person

They look similar, but they have different jobs.
