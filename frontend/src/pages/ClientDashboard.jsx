import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'

export default function ClientDashboard() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get(`/device/user/${auth.userId}`)
      .then(r => setDevices(r.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [auth.userId])

  const handleLogout = () => { logout(); navigate('/login') }

  return (
    <>
      <nav>
        <h1>Energy Management</h1>
        <div className="nav-right">
          <span>{auth.username}</span>
          <span className="badge badge-client">CLIENT</span>
          <button className="btn btn-danger btn-sm" onClick={handleLogout}>Logout</button>
        </div>
      </nav>

      <div className="page">
        <div className="card">
          <h2 style={{ marginBottom:'1rem' }}>My Devices</h2>
          {loading ? <p>Loading…</p> : devices.length === 0 ? (
            <p style={{ color:'#94a3b8' }}>No devices assigned to your account.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Description</th>
                  <th>Max Consumption (W)</th>
                </tr>
              </thead>
              <tbody>
                {devices.map(d => (
                  <tr key={d.id}>
                    <td>{d.name}</td>
                    <td>{d.description || '—'}</td>
                    <td>{d.maxConsumption}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </>
  )
}
