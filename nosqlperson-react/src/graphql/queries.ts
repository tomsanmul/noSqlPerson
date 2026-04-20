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
