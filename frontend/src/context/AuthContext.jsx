import React, { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const stored = sessionStorage.getItem('auth')
    return stored ? JSON.parse(stored) : null
  })

  const login = (data) => {

    sessionStorage.setItem('auth', JSON.stringify(data))
    setAuth(data)
  }

  const logout = () => {
    sessionStorage.removeItem('auth')
    setAuth(null)
  }

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
