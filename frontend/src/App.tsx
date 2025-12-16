import { useState } from 'react'
import { Routes, Route, Link } from 'react-router-dom'
import FileUpload from './components/FileUpload'
import DocumentList from './components/DocumentList/DocumentList'
import './App.css'

function App() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

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
          <h1>Recall</h1>
          <Routes>
            <Route path="/" element={<FileUpload />} />
          </Routes>
        </div>
      </div>
    </div>
  )
}

export default App