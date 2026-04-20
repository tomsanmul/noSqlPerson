# personNoSql-react-apollo

## Summary



> This project introduces a clean and simple <mark>React + Vite</mark> sandbox built with <mark>Apollo Client</mark> to consume a <mark>Spring Boot GraphQL API</mark>.
> 
> Designed as a lightweight testing environment, it features a well-structured Apollo Client setup with custom **middleware** for logging requests and handling errors.
> 
>  **GraphQL** operations — including **queries**, **mutations**, and **subscriptions** — are neatly organized in dedicated files.

## Guide

Here's a **step-by-step guide** to create the **React + Vite + Apollo Client** sandbox project that connects to our<mark> Spring Boot GraphQL server</mark>.

### Step 1: Create the React Vite Project

Open your terminal and run:

```bash
npm create vite@latest person-graphql-client -- --template react
cd person-graphql-client
npm install
```

### Step 2: Install Apollo Client & Dependencies

```bash
npm install @apollo/client graphql
npm install --save-dev @types/node   # optional, for better env support
```

### Step 3: Project Structure (Recommended)

After creation, organize it like this:

```
person-graphql-client/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── graphql/              # ← New folder
│   │   ├── queries.ts
│   │   ├── mutations.ts
│   │   └── subscriptions.ts
│   ├── client.ts             # ← Apollo Client setup + middleware
│   └── components/
│       └── PersonList.tsx
```

### Step 4: Create Apollo Client with Middleware (`src/client.ts`)

```ts
// src/client.ts
import { ApolloClient, InMemoryCache, HttpLink, ApolloLink, concat } from '@apollo/client';
import { onError } from '@apollo/client/link/error';

const httpLink = new HttpLink({
  uri: 'http://localhost:8080/graphql',   // ← Change if your Spring Boot port is different
});

// Example: Simple Logging Middleware
const authMiddleware = new ApolloLink((operation, forward) => {
  console.log(`[GraphQL Request] ${operation.operationName}`);
  console.log('Variables:', operation.variables);

  // You can add headers here if needed later (auth, etc.)
  // operation.setContext({
  //   headers: { Authorization: `Bearer ${token}` }
  // });

  return forward(operation);
});

// Error handling middleware
const errorLink = onError(({ graphQLErrors, networkError }) => {
  if (graphQLErrors) {
    graphQLErrors.forEach(({ message, locations, path }) =>
      console.error(`[GraphQL error]: Message: ${message}, Path: ${path}`)
    );
  }
  if (networkError) {
    console.error(`[Network error]: ${networkError}`);
  }
});

export const client = new ApolloClient({
  link: concat(errorLink, concat(authMiddleware, httpLink)),
  cache: new InMemoryCache(),
});
```

### Step 5: Define GraphQL Operations

#### `src/graphql/queries.ts`

```ts
import { gql } from '@apollo/client';

export const GET_ALL_PERSONS = gql`
  query GetAllPersons {
    allPersons {
      id
      operation
      name
      age
      email
      createdAt
      updatedAt
    }
  }
`;

export const GET_PERSON_BY_KEY = gql`
  query GetPersonByKey($id: ID!, $operation: String!) {
    personByKey(id: $id, operation: $operation) {
      id
      operation
      name
      age
      email
    }
  }
`;
```

#### `src/graphql/mutations.ts`

```ts
import { gql } from '@apollo/client';

export const SAVE_PERSON = gql`
  mutation SavePerson($person: person!) {
    savePerson(person: $person) {
      id
      operation
      name
      age
      email
    }
  }
`;

export const UPDATE_PERSON = gql`
  mutation UpdatePerson($person: person!) {
    updatePerson(person: $person) {
      id
      operation
      name
      age
      email
    }
  }
`;

export const DELETE_PERSON = gql`
  mutation DeletePerson($id: ID!, $operation: String!) {
    deletePersonByKey(id: $id, operation: $operation) {
      id
      operation
      name
    }
  }
`;
```

#### `src/graphql/subscriptions.ts`

```ts
import { gql } from '@apollo/client';

export const PERSON_CREATED = gql`
  subscription PersonCreated {
    personCreated {
      id
      operation
      name
      age
      email
    }
  }
`;
```

### Step 6: Update `main.tsx`

```ts
// src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import { ApolloProvider } from '@apollo/client';
import { client } from './client.ts';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ApolloProvider client={client}>
      <App />
    </ApolloProvider>
  </React.StrictMode>
);
```

### Step 7: Simple Test App (`src/App.tsx`)

```ts
// src/App.tsx
import { useQuery, useMutation } from '@apollo/client';
import { GET_ALL_PERSONS, SAVE_PERSON } from './graphql/queries';
import { useState } from 'react';

function App() {
  const [name, setName] = useState('');
  const [age, setAge] = useState(0);
  const [operation, setOperation] = useState('CREATE');

  const { data, loading, error, refetch } = useQuery(GET_ALL_PERSONS);

  const [savePerson] = useMutation(SAVE_PERSON, {
    onCompleted: () => {
      refetch();
      setName('');
      setAge(0);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    savePerson({
      variables: {
        person: {
          operation,
          name,
          age,
          email: `${name.toLowerCase()}@example.com`,
        },
      },
    });
  };

  if (loading) return <p>Loading persons...</p>;
  if (error) return <p>Error: {error.message}</p>;

  return (
    <div style={{ padding: '20px' }}>
      <h1>Person GraphQL Sandbox</h1>

      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          type="number"
          placeholder="Age"
          value={age}
          onChange={(e) => setAge(Number(e.target.value))}
        />
        <button type="submit">Save Person</button>
      </form>

      <h2>Persons List</h2>
      <ul>
        {data?.allPersons.map((person: any) => (
          <li key={`${person.id}-${person.operation}`}>
            {person.name} ({person.age}) - {person.email}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default App;
```

### Step 8: Run Both Projects

1. We need to start our **Spring Boot GraphQL server** (usually on port 8080)
2. After that, we start the React client:

```bash
npm run dev
```

Open `http://localhost:5173`

We should now be able to:

- See all persons
- Create new persons
- The middleware logs every request in the console
