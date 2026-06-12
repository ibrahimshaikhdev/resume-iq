import Home from "./home/home.jsx";
import Login from "./login/login.jsx";
import "./App.css"
import { ToastContainer } from "react-toastify";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { useContext } from "react";
import { usercontext } from "./appcontext.jsx";
import Forgotpassword from "./resetpassword/resetpassword.jsx";
import Uploadpage from "./upload/upload.jsx";
import JobProgress from "./upload/JobProgress.jsx";
import Analyse from "./analyse/analyse.jsx";
import Dashboard from "./dashboard/Dashboard.jsx";
import AdminDashboard from "./admin/AdminDashboard.jsx";
import RecruiterSimulation from "./analyse/RecruiterSimulation.jsx";
import FreeAtsResult from "./analyse/FreeAtsResult.jsx";
import ErrorBoundary from "./components/ErrorBoundary.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import Styles from "./loadstyle.module.css"

function App() {

  const { isauthenticated } = useContext(usercontext)
  return (isauthenticated ?
    <>
      <ToastContainer theme="dark" stacked autoClose={1500} />
      <BrowserRouter>
        <ErrorBoundary>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/forgotpassword" element={<Forgotpassword />} />
            <Route path="/uploaddoc" element={<Uploadpage />} />
            <Route path="/jobprogress" element={<JobProgress />} />
            <Route path="/analysereport" element={<ProtectedRoute><Analyse /></ProtectedRoute>} />
            <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
            <Route path="/admin" element={<ProtectedRoute><AdminDashboard /></ProtectedRoute>} />
            <Route path="/simulation" element={<ProtectedRoute><RecruiterSimulation /></ProtectedRoute>} />
            <Route path="/free-result" element={<FreeAtsResult />} />
          </Routes>
        </ErrorBoundary>
      </BrowserRouter>
    </> :
    <div className={Styles.loadani} id="animate">
      <div className={Styles.loadanimation}>
        <div className={Styles.capstart}></div>
        <div className={Styles.loadblock}></div>
      </div>
      <h1>ResumeIQ</h1>
    </div>
  )
}

export default App
