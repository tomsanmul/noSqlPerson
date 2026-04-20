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
