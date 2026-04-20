import { useState, useEffect } from 'react'
import { client } from './client'
import { GET_ALL_PERSONS } from './graphql/queries'
import { SAVE_PERSON } from './graphql/mutations'
import './App.css'

interface Person {
  id: string
  operation: string
  name: string
  age: number
  email: string
}

function App() {
  const [persons, setPersons] = useState<Person[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [age, setAge] = useState(0)
  const [operation] = useState('PERSON')

  const fetchPersons = async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await client.query({
        query: GET_ALL_PERSONS,
        fetchPolicy: 'network-only',
      })
      setPersons(result.data.allPersons)
      setLoading(false)
    } catch (err: any) {
      setError(err.message)
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPersons()
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    try {
      const result = await client.mutate({
        mutation: SAVE_PERSON,
        variables: {
          person: {
            operation,
            name,
            age,
            email: `${name.toLowerCase()}@example.com`,
          },
        },
      })
      console.log('Mutation result:', result)
      console.log('Saved person:', result.data?.savePerson)
      setName('')
      setAge(0)
      fetchPersons()
    } catch (err: any) {
      setError(err.message)
    }
  }

  if (loading) return <p>Loading persons...</p>
  if (error) return <p>Error: {error}</p>

  return (
    <div style={{ padding: '20px' }}>
      <h1>Person GraphQL Sandbox</h1>

      <form onSubmit={handleSubmit}>
        <hr />
        <h2>Create Person</h2>
        <input
          type="text"
          placeholder="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        /><br />
        <input
          type="number"
          placeholder="Age"
          value={age}
          onChange={(e) => setAge(Number(e.target.value))}
        /><br />
        <button type="submit">Save Person</button>
      </form>

      <hr />
      <h2>Persons List</h2>
      <div>
        {persons.map((person) => (
          <div key={`${person.id}-${person.operation}`}>
            {person.name} ({person.age}) - {person.email}
          </div>
        ))}
      </div>
    </div>
  )
}

export default App
