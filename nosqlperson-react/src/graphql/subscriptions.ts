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
