import { useState } from 'react'
import { Routes, Route, Link, useLocation } from 'react-router-dom'
import FileUpload from './components/FileUpload'
import DocumentList from './components/DocumentList/DocumentList'
import Chat from './components/Chat/Chat'
import './App.css'

function App() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const isChatPage = location.pathname.startsWith('/chat/')

  return (
    <div className="App">
      <div className="top-bar">
        <button
          className="sidebar-toggle"
          onClick={() => setSidebarOpen(!sidebarOpen)}
        >
          📄 Documents
        </button>
        <Link to="/" className="top-bar-link">
          📤 Upload
        </Link>
      </div>

      <div className="main-layout">
        {sidebarOpen && (
          <div className="sidebar">
            <DocumentList />
          </div>
        )}

        <div className="main-content">
          {!isChatPage && <h1>Recall</h1>}
          <Routes>
            <Route path="/" element={<FileUpload />} />
            <Route path="/chat/:documentId" element={<Chat />} />
          </Routes>
        </div>
      </div>
    </div>
  )
}

export default App