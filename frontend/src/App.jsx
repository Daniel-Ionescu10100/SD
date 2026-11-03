import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import AdminDashboard from './pages/AdminDashboard'
import ClientDashboard from './pages/ClientDashboard'

function ProtectedRoute({ children, requiredRole }) {
  const { auth } = useAuth()
  if (!auth) return <Navigate to="/login" replace />
  if (requiredRole && auth.role !== requiredRole) return <Navigate to="/" replace />
  return children
}

function RootRedirect() {
  const { auth } = useAuth()
  if (!auth) return <Navigate to="/login" replace />
  return auth.role === 'ADMIN'
    ? <Navigate to="/admin" replace />
    : <Navigate to="/client" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/admin" element={
            <ProtectedRoute requiredRole="ADMIN"><AdminDashboard /></ProtectedRoute>
          } />
          <Route path="/client" element={
            <ProtectedRoute requiredRole="CLIENT"><ClientDashboard /></ProtectedRoute>
          } />
          <Route path="*" element={<RootRedirect />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
