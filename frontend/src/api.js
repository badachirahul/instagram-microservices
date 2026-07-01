import axios from 'axios'

// The frontend talks ONLY to the gateway (:8080), which routes to the services
// and handles CORS for :5173. Override with VITE_API_BASE if needed.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8080',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  // guard against junk values ("undefined"/"" from a stale session)
  if (token && token !== 'undefined' && token !== 'null') {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Self-heal: any 401 means the stored session is missing/expired/invalid — clear
// it and return to the login screen so a fresh dev-login can mint a good token.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.clear()
      if (!window.location.hash.includes('relogin')) {
        window.location.hash = 'relogin'
        window.location.reload()
      }
    }
    return Promise.reject(err)
  },
)

export default api
