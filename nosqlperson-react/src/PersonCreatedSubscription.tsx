import { useEffect, useState } from 'react';
import { PERSON_CREATED } from './graphql/subscriptions';
import { client } from './client';

interface Person {
    id: string
    operation: string
    name: string
    age: number
    email: string
}

export default function PersonCreatedSubscription() {
    const [persons, setPersons] = useState<Person[]>([]);
    const [isConnected, setIsConnected] = useState(false);
    //const [isError, setIsError] = useState(false)

    useEffect(() => {
        // Start the subscription
        const subscription = client
            .subscribe({
                query: PERSON_CREATED,
                fetchPolicy: 'no-cache',
            })
            .subscribe({
                next: ({ data }) => {
                    if (data?.personCreated) {
                        setPersons((prev) => [data.personCreated, ...prev]); // newest on top
                    }
                },
                error: (err) => {
                    console.error('Subscription error:', err);
                },
            });

        setIsConnected(true);

        // Cleanup when component unmounts
        return () => {
            subscription.unsubscribe();
            setIsConnected(false);
        };
    }, [client]);

    return (
        <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
            <h2>Person Created: Subscription</h2>
            <p>Status: {isConnected ? '✅ Listening for new persons...' : 'Disconnected'}</p>

            <h3>New Persons ({persons.length})</h3>

            {persons.length === 0 ? (
                <p>No persons created yet. Waiting for new data...</p>
            ) : (
                <ul style={{ padding: 0, listStyle: 'none' }}>
                    {persons.map((person) => (
                        <li
                            key={person.id}
                            style={{
                                padding: '12px',
                                margin: '8px 0',
                                border: '1px solid #ccc',
                                borderRadius: '6px',
                                backgroundColor: '#f8f9fa',
                            }}
                        >
                            <strong>{person.name}</strong> — {person.age} years old
                            <br />
                            <em>{person.email}</em>
                            <br />
                            <small>Operation: {person.operation}</small>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}