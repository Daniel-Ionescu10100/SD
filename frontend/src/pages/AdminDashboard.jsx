import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'

function UserModal({ user, onClose, onSaved }) {
  const isEdit = !!user?.id
  const [form, setForm] = useState({
    name: user?.name || '',
    username: user?.username || '',
    password: '',
    role: user?.role || 'CLIENT',
  })
  const [err, setErr]   = useState('')

  const handleChange = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async e => {
    e.preventDefault(); setErr('')
    try {
      if (isEdit) {
        const payload = { ...form }
        if (!payload.password || !payload.password.trim()) {
          delete payload.password
        }
        await api.put(`/user/${user.id}`, payload)
      } else {
        await api.post('/user', form)
      }
      onSaved()
    } catch (ex) {
      const msg = ex.response?.data?.details?.join(', ') || ex.response?.data?.message || 'Error saving user'
      setErr(msg)
    }
  }

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>{isEdit ? 'Edit User' : 'Create User'}</h3>
        <form onSubmit={handleSubmit}>
          <input name="name"     value={form.name}     onChange={handleChange} placeholder="Full name" required />
          <input name="username" value={form.username}  onChange={handleChange} placeholder="Username" required />
          <input
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            placeholder={isEdit ? "Password (leave blank to keep current)" : "Password"}
            required={!isEdit}
          />
          <select name="role" value={form.role} onChange={handleChange}>
            <option value="CLIENT">CLIENT</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          {err && <p className="error-msg">{err}</p>}
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary">{isEdit ? 'Save' : 'Create'}</button>
          </div>
        </form>
      </div>
    </div>
  )
}

function DeviceModal({ device, users, onClose, onSaved }) {
  const isEdit = !!device?.id
  const [form, setForm] = useState({
    name: device?.name || '',
    description: device?.description || '',
    maxConsumption: device?.maxConsumption ?? '',
    userId: device?.userId || '',
  })
  const [err, setErr]   = useState('')

  const handleChange = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async e => {
    e.preventDefault(); setErr('')
    try {
      const payload = {
        name: form.name,
        description: form.description,
        maxConsumption: parseFloat(form.maxConsumption),
        userId: form.userId || null,
      }
      if (isEdit) {
        await api.put(`/device/${device.id}`, payload)
      } else {
        await api.post('/device', payload)
      }
      onSaved()
    } catch (ex) {
      const msg = ex.response?.data?.details?.join(', ') || ex.response?.data?.message || 'Error saving device'
      setErr(msg)
    }
  }

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>{isEdit ? 'Edit Device' : 'Create Device'}</h3>
        <form onSubmit={handleSubmit}>
          <input name="name" value={form.name} onChange={handleChange} placeholder="Device name" required />
          <input name="description" value={form.description} onChange={handleChange} placeholder="Description" />
          <input name="maxConsumption" type="number" step="0.1" value={form.maxConsumption} onChange={handleChange} placeholder="Max consumption (W)" required />
          <select name="userId" value={form.userId || ''} onChange={handleChange}>
            <option value="">— Unassigned —</option>
            {users.map(u => <option key={u.id} value={u.id}>{u.name} ({u.username})</option>)}
          </select>
          {err && <p className="error-msg">{err}</p>}
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary">{isEdit ? 'Save' : 'Create'}</button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function AdminDashboard() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  const [tab,     setTab]     = useState('users')
  const [users,   setUsers]   = useState([])
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(false)

  const [userModal,   setUserModal]   = useState(null)
  const [deviceModal, setDeviceModal] = useState(null)

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try { setUsers((await api.get('/user')).data) }
    catch(e) { console.error(e) }
    finally { setLoading(false) }
  }, [])

  const fetchDevices = useCallback(async () => {
    setLoading(true)
    try { setDevices((await api.get('/device')).data) }
    catch(e) { console.error(e) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchUsers(); fetchDevices() }, [fetchUsers, fetchDevices])

  const deleteUser = async id => {
    if (!window.confirm('Delete this user?')) return
    try {
      await api.delete(`/user/${id}`)
      fetchUsers()
      fetchDevices()
    } catch(e) {
      alert(e.response?.data?.details?.join(', ') || e.response?.data?.message || 'Delete user failed')
    }
  }

  const deleteDevice = async id => {
    if (!window.confirm('Delete this device?')) return
    try {
      await api.delete(`/device/${id}`)
      fetchDevices()
    } catch(e) {
      alert(e.response?.data?.details?.join(', ') || e.response?.data?.message || 'Delete device failed')
    }
  }

  const handleLogout = () => { logout(); navigate('/login') }

  const userById = id => users.find(u => u.id === id)

  return (
    <>
      <nav>
        <h1>Energy Management — Admin</h1>
        <div className="nav-right">
          <a href="/docs" target="_blank" rel="noreferrer" className="btn btn-sm" style={{ background: '#312e81', color: '#c7d2fe', textDecoration: 'none', display: 'flex', alignItems: 'center' }}>Swagger Docs</a>
          <span>{auth.username}</span>
          <span className="badge badge-admin">ADMIN</span>
          <button className="btn btn-danger btn-sm" onClick={handleLogout}>Logout</button>
        </div>
      </nav>

      <div className="page">
        <div className="tabs">
          <button className={`tab ${tab==='users'   ? 'active':''}`} onClick={()=>setTab('users')}>Users</button>
          <button className={`tab ${tab==='devices' ? 'active':''}`} onClick={()=>setTab('devices')}>Devices</button>
        </div>

        {tab === 'users' && (
          <div className="card">
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem' }}>
              <h2>Users</h2>
              <button className="btn btn-success btn-sm" onClick={()=>setUserModal({})}>+ New User</button>
            </div>
            {loading ? <p>Loading…</p> : (
              <table>
                <thead><tr><th>Name</th><th>Username</th><th>Role</th><th>Actions</th></tr></thead>
                <tbody>
                  {users.map(u => (
                    <tr key={u.id}>
                      <td>{u.name}</td>
                      <td>{u.username}</td>
                      <td><span className={`badge ${u.role==='ADMIN'?'badge-admin':'badge-client'}`}>{u.role}</span></td>
                      <td style={{ display:'flex', gap:'0.4rem' }}>
                        <button className="btn btn-primary btn-sm" onClick={()=>setUserModal(u)}>Edit</button>
                        <button className="btn btn-danger btn-sm" onClick={()=>deleteUser(u.id)}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {tab === 'devices' && (
          <div className="card">
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem' }}>
              <h2>Devices</h2>
              <button className="btn btn-success btn-sm" onClick={()=>setDeviceModal({})}>+ New Device</button>
            </div>
            {loading ? <p>Loading…</p> : (
              <table>
                <thead><tr><th>Name</th><th>Description</th><th>Max W</th><th>Assigned To</th><th>Actions</th></tr></thead>
                <tbody>
                  {devices.map(d => {
                    const owner = d.userId ? userById(d.userId) : null
                    return (
                      <tr key={d.id}>
                        <td>{d.name}</td>
                        <td>{d.description || '—'}</td>
                        <td>{d.maxConsumption}</td>
                        <td>{owner ? `${owner.name} (${owner.username})` : <em style={{color:'#94a3b8'}}>Unassigned</em>}</td>
                        <td style={{ display:'flex', gap:'0.4rem' }}>
                          <button className="btn btn-primary btn-sm" onClick={()=>setDeviceModal(d)}>Edit</button>
                          <button className="btn btn-danger btn-sm" onClick={()=>deleteDevice(d.id)}>Delete</button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>

      {userModal !== null && (
        <UserModal
          user={userModal.id ? userModal : null}
          onClose={() => setUserModal(null)}
          onSaved={() => { setUserModal(null); fetchUsers(); fetchDevices() }}
        />
      )}

      {deviceModal !== null && (
        <DeviceModal
          device={deviceModal.id ? deviceModal : null}
          users={users}
          onClose={() => setDeviceModal(null)}
          onSaved={() => { setDeviceModal(null); fetchDevices() }}
        />
      )}
    </>
  )
}
