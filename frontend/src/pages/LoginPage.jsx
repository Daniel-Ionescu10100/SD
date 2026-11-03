import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate   = useNavigate()

  const [form,    setForm]    = useState({ username: '', password: '' })
  const [error,   setError]   = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async e => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {

      const res = await api.post('/auth/login', {
        username: form.username,
        password: form.password,
      })
      login(res.data)
      navigate(res.data.role === 'ADMIN' ? '/admin' : '/client')
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid username or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', minHeight:'100vh' }}>
      <div className="card" style={{ width: 380 }}>
        <h2 style={{ marginBottom:'1.5rem', textAlign:'center', color:'#4f46e5' }}>
          Energy Management
        </h2>
        <form onSubmit={handleSubmit}>
          <label style={{ fontSize:'0.85rem', fontWeight:600, display:'block', marginBottom:'0.3rem' }}>
            Username
          </label>
          <input
            name="username"
            value={form.username}
            onChange={handleChange}
            placeholder="Enter username"
            required
            autoFocus
          />
          <label style={{ fontSize:'0.85rem', fontWeight:600, display:'block', marginBottom:'0.3rem' }}>
            Password
          </label>
          <input
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            placeholder="Enter password"
            required
          />
          {error && <p className="error-msg">{error}</p>}
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width:'100%', marginTop:'1rem', padding:'0.75rem' }}
            disabled={loading}
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  )
}
