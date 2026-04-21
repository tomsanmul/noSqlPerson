# Apollo Client Configuration Changes: Adding WebSocket Support

## Overview

> The client configuration was updated to support **GraphQL subscriptions** via <mark>WebSocket</mark>, while maintaining <mark>HTTP for queries and mutations.</mark>

## Before: HTTP Only

```typescript
import { ApolloClient, InMemoryCache, HttpLink } from '@apollo/client';

export const client = new ApolloClient({
  link: new HttpLink({ uri: 'http://localhost:8080/graphql' }),
  cache: new InMemoryCache(),
});
```

**Limitation:** Only supports queries and mutations over HTTP. <mark>Subscriptions don't work.</mark>

## After: HTTP + WebSocket with Split Link

### Installing dependency

What we need to do: **Install the `graphql-ws` package** using the package manager:

```bash
npm install graphql-ws 
# or yarn add graphql-ws 
# or pnpm add graphql-ws`
```

> Once installed, our **subscriptions** will be able to be coded. 
> 
> The split link automatically detects subscription operations and routes them through the WebSocket connection, while queries and mutations continue using HTTP.

### Code

```typescript
import { ApolloClient, InMemoryCache, HttpLink, split } from '@apollo/client';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { createClient } from 'graphql-ws';

// HTTP link for queries and mutations
const httpLink = new HttpLink({
  uri: 'http://localhost:8080/graphql',
});

// WebSocket link for subscriptions
const wsLink = new GraphQLWsLink(
  createClient({
    url: 'ws://localhost:8080/graphql',
  })
);

// Split link: routes operations based on type
const splitLink = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    return (
      definition.kind === 'OperationDefinition' &&
      definition.operation === 'subscription'
    );
  },
  wsLink,    // Use WebSocket for subscriptions
  httpLink   // Use HTTP for queries/mutations
);

export const client = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache(),
});
```

### Key Changes

#### 1. **New Dependencies**

- `split` - Routes operations to different links
- `GraphQLWsLink` - Apollo's WebSocket link adapter
- `getMainDefinition` - Extracts operation type from GraphQL query
- `createClient` from `graphql-ws` - Creates WebSocket client

#### 2. **Dual Transport Setup**

- **`httpLink`** - Handles queries and mutations via HTTP POST
- **`wsLink`** - Handles subscriptions via WebSocket connection

#### 3. **Smart Routing with `split()`**

The `split` function acts as a router:

```typescript
split(
  testFunction,  // Returns true/false
  ifTrue,        // Use this link if true (wsLink)
  ifFalse        // Use this link if false (httpLink)
)
```

**Test logic:**

- Checks if operation is a `subscription` → routes to WebSocket
- Otherwise (query/mutation) → routes to HTTP

#### 4. **Protocol Endpoints**

- **HTTP:** `http://localhost:8080/graphql`
- **WebSocket:** `ws://localhost:8080/graphql`

### How It Works

```
┌─────────────────┐
│  GraphQL Query  │
└────────┬────────┘
         │
         ▼
    ┌────────┐
    │ split  │ ◄── Examines operation type
    └───┬────┘
        │
        ├─── subscription? ──► wsLink ──► ws://localhost:8080/graphql
        │
        └─── query/mutation? ──► httpLink ──► http://localhost:8080/graphql
```

Benefits

✅ **Real-time updates** - Subscriptions work via persistent WebSocket connection  
✅ **Efficient** - HTTP used for one-off operations, WebSocket only for subscriptions  
✅ **Automatic routing** - No manual intervention needed per operation  
✅ **Standard pattern** - Official Apollo Client best practice

## What is Split Link?

<mark>Definition</mark>:

> **Split Link** is an Apollo Client utility that **routes GraphQL operations to different network transports** based on the operation type.

Think of it as a **traffic router** for your GraphQL requests.

### The Problem It Solves

Different GraphQL operations need different protocols:

| Operation        | Best Protocol | Why                                         |
| ---------------- | ------------- | ------------------------------------------- |
| **Query**        | HTTP          | One-time request/response                   |
| **Mutation**     | HTTP          | One-time request/response                   |
| **Subscription** | WebSocket     | Persistent connection for real-time updates |

> Without split link, you'd need to manually choose which client to use for each operation.

<mark>Visual Flow</mark>

```
User makes GraphQL request
         │
         ▼
    ┌─────────┐
    │  split  │ ← Inspects the operation
    └─────────┘
         │
         ├─ Is it a subscription?
         │
    YES  │  NO
     │   │   │
     ▼   │   ▼
  wsLink │ httpLink
     │   │   │
     ▼   │   ▼
    WS   │  HTTP
         │
         ▼
    Response back to app
```

When we call:

```typescript
// This goes through httpLink (HTTP)
client.query({ query: GET_ALL_PERSONS })

// This goes through httpLink (HTTP)
client.mutate({ mutation: SAVE_PERSON, variables: {...} })

// This goes through wsLink (WebSocket)
client.subscribe({ query: PERSON_CREATED })
```

The split link **automatically** routes each to the correct transport.

### The Test Function

```typescript
({ query }) => {
  const definition = getMainDefinition(query);
  return (
    definition.kind === 'OperationDefinition' &&
    definition.operation === 'subscription'
  );
}
```

This function:

1. **Extracts** the main definition from the GraphQL query
2. **Checks** if it's an operation definition (not a fragment)
3. **Tests** if the operation type is `'subscription'`
4. **Returns** `true` for subscriptions, `false` for queries/mutations
