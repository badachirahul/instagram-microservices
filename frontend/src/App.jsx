import { useEffect, useState } from 'react'
import api from './api'
import { devLogin } from './devAuth'

export default function App() {
  const [session, setSession] = useState(() => {
    const token = localStorage.getItem('token')
    return token
      ? { token, username: localStorage.getItem('username'), userId: localStorage.getItem('userId') }
      : null
  })

  if (!session) return <Login onLogin={setSession} />
  return <Home session={session} onLogout={() => { localStorage.clear(); setSession(null) }} />
}

function Login({ onLogin }) {
  const [username, setUsername] = useState('rahul')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    if (!username.trim()) return
    setBusy(true)
    const s = await devLogin(username.trim())
    localStorage.setItem('token', s.token)
    localStorage.setItem('username', s.username)
    localStorage.setItem('userId', s.userId)
    onLogin(s)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <form onSubmit={submit} className="w-80 bg-white border rounded-xl p-6 shadow-sm space-y-4">
        <h1 className="text-2xl font-semibold text-center">Instagram</h1>
        <p className="text-xs text-center text-gray-400">dev login — mints a JWT locally</p>
        <input
          className="w-full border rounded-lg px-3 py-2 text-sm"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="username"
        />
        <button disabled={busy} className="w-full bg-blue-500 text-white rounded-lg py-2 text-sm font-medium disabled:opacity-50">
          {busy ? '…' : 'Enter'}
        </button>
      </form>
    </div>
  )
}

function Home({ session, onLogout }) {
  const [posts, setPosts] = useState([])
  const [error, setError] = useState(null)

  async function loadFeed() {
    try {
      const { data } = await api.get('/api/feed')
      setPosts(data)
      setError(null)
    } catch (e) {
      setError(e?.message || 'Failed to load feed')
    }
  }

  useEffect(() => { loadFeed() }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="sticky top-0 bg-white border-b">
        <div className="max-w-xl mx-auto flex items-center justify-between px-4 py-3">
          <span className="font-semibold text-lg">Instagram</span>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-gray-500">@{session.username}</span>
            <button onClick={onLogout} className="text-blue-500">logout</button>
          </div>
        </div>
      </header>

      <main className="max-w-xl mx-auto px-4 py-6 space-y-6">
        <CreatePost onCreated={loadFeed} />
        {error && <p className="text-red-500 text-sm">{error}</p>}
        {posts.length === 0 && !error && <p className="text-gray-400 text-sm text-center">No posts yet.</p>}
        {posts.map((p) => (
          <PostCard key={p.id} post={p} session={session} onChange={loadFeed} />
        ))}
      </main>
    </div>
  )
}

function CreatePost({ onCreated }) {
  const [imageUrl, setImageUrl] = useState('')
  const [caption, setCaption] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    if (!imageUrl.trim()) return
    setBusy(true)
    try {
      await api.post('/api/posts', { imageUrl: imageUrl.trim(), caption: caption.trim() })
      setImageUrl(''); setCaption('')
      onCreated()
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="bg-white border rounded-xl p-4 space-y-3">
      <h2 className="text-sm font-medium">New post</h2>
      <input
        className="w-full border rounded-lg px-3 py-2 text-sm"
        placeholder="image URL (paste any https image link)"
        value={imageUrl}
        onChange={(e) => setImageUrl(e.target.value)}
      />
      <input
        className="w-full border rounded-lg px-3 py-2 text-sm"
        placeholder="caption"
        value={caption}
        onChange={(e) => setCaption(e.target.value)}
      />
      <button disabled={busy} className="bg-blue-500 text-white rounded-lg px-4 py-2 text-sm font-medium disabled:opacity-50">
        {busy ? 'Posting…' : 'Share'}
      </button>
    </form>
  )
}

function PostCard({ post, session, onChange }) {
  const [showComments, setShowComments] = useState(false)
  const [comments, setComments] = useState([])
  const [text, setText] = useState('')

  async function toggleLike() {
    if (post.likedByMe) await api.delete(`/api/posts/${post.id}/like`)
    else await api.post(`/api/posts/${post.id}/like`)
    onChange()
  }

  async function loadComments() {
    const { data } = await api.get(`/api/posts/${post.id}/comments`)
    setComments(data)
  }

  async function openComments() {
    const next = !showComments
    setShowComments(next)
    if (next) await loadComments()
  }

  async function addComment(e) {
    e.preventDefault()
    if (!text.trim()) return
    await api.post(`/api/posts/${post.id}/comments`, { text: text.trim() })
    setText('')
    await loadComments()
    onChange()
  }

  return (
    <article className="bg-white border rounded-xl overflow-hidden">
      <div className="px-4 py-3 text-sm font-medium">@{post.authorUsername}</div>
      <img
        src={post.imageUrl}
        alt={post.caption || 'post'}
        className="w-full max-h-[28rem] object-cover bg-gray-100"
        onError={(e) => { e.currentTarget.src = 'https://placehold.co/600x400?text=image' }}
      />
      <div className="px-4 py-3 space-y-2">
        <div className="flex items-center gap-4 text-sm">
          <button onClick={toggleLike} className={post.likedByMe ? 'text-red-500 font-medium' : 'text-gray-700'}>
            {post.likedByMe ? '♥' : '♡'} {post.likeCount}
          </button>
          <button onClick={openComments} className="text-gray-700">
            💬 {post.commentCount}
          </button>
        </div>
        {post.caption && (
          <p className="text-sm"><span className="font-medium">@{post.authorUsername}</span> {post.caption}</p>
        )}

        {showComments && (
          <div className="pt-2 space-y-2 border-t">
            {comments.map((c) => (
              <p key={c.id} className="text-sm"><span className="font-medium">@{c.username}</span> {c.text}</p>
            ))}
            <form onSubmit={addComment} className="flex gap-2 pt-1">
              <input
                className="flex-1 border rounded-lg px-3 py-1.5 text-sm"
                placeholder="Add a comment…"
                value={text}
                onChange={(e) => setText(e.target.value)}
              />
              <button className="text-blue-500 text-sm font-medium">Post</button>
            </form>
          </div>
        )}
      </div>
    </article>
  )
}
