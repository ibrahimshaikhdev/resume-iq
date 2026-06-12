import { useContext, useEffect, useState } from "react"
import Styles from "./Dashboard.module.css"
import LoadStyles from "../loadstyle.module.css"
import { usercontext } from "../appcontext"
import { useNavigate } from "react-router-dom"
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts"
import Footer from "../components/footer.jsx"

function Dashboard() {
    const navigate = useNavigate()
    const { serviceURL } = useContext(usercontext)
    const [analysisHistory, setAnalysisHistory] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        document.getElementById("animate").style.display = "flex"
        fetch(`${serviceURL}/history`, { credentials: "include" })
            .then(r => r.ok ? r.json() : [])
            .then(analysis => {
                setAnalysisHistory(analysis || [])
                setLoading(false)
                document.getElementById("animate").style.display = "none"
            }).catch(() => {
                setLoading(false)
                document.getElementById("animate").style.display = "none"
            })
    }, [])

    const formatDate = (dateStr) => {
        if (!dateStr) return "N/A"
        const d = new Date(dateStr)
        return d.toLocaleDateString("en-US", { month: "short", day: "numeric" })
    }

    const formatDateTime = (dateStr) => {
        if (!dateStr) return "N/A"
        const d = new Date(dateStr)
        return d.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit" })
    }

    // Chart data
    const chartData = [...analysisHistory].reverse().map(r => ({
        date: formatDate(r.analyzedAt),
        score: r.score,
        atsScore: r.atsoptimizationscore
    }))

    // Stats
    const totalAnalyses = analysisHistory.length
    const bestScore = analysisHistory.length > 0 ? Math.max(...analysisHistory.map(r => r.score)) : 0
    const avgScore = analysisHistory.length > 0 ? Math.round(analysisHistory.reduce((s, r) => s + r.score, 0) / analysisHistory.length) : 0

    const handleDownloadPdf = (id) => {
        fetch(`${serviceURL}/report/${id}/pdf`, { credentials: "include" })
            .then(res => {
                if (!res.ok) throw new Error("Failed")
                return res.blob()
            })
            .then(blob => {
                const url = URL.createObjectURL(blob)
                const a = document.createElement("a")
                a.href = url
                a.download = "resumeiq-report.pdf"
                a.click()
                URL.revokeObjectURL(url)
            })
            .catch(() => {})
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <span className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</span>
                <div className={Styles.navLinks}>
                    <button className={Styles.navbtn} onClick={() => navigate("/uploaddoc")}>
                        <i className="fa-solid fa-plus"></i> New Analysis
                    </button>
                    <button className={Styles.navbtn} onClick={() => navigate("/")}>
                        <i className="fa-solid fa-house"></i> Home
                    </button>
                </div>
            </nav>

            <div className={Styles.content}>
                <h1 className={Styles.pageTitle}>
                    <i className="fa-solid fa-chart-line"></i> Dashboard
                </h1>

                {/* Stats Cards */}
                <div className={Styles.statsGrid}>
                    <div className={Styles.statCard}>
                        <div className={Styles.statIcon} style={{ background: "rgba(124, 58, 237, 0.15)" }}>
                            <i className="fa-solid fa-file-lines" style={{ color: "#a78bfa" }}></i>
                        </div>
                        <div className={Styles.statInfo}>
                            <span className={Styles.statNum}>{totalAnalyses}</span>
                            <span className={Styles.statLabel}>Total Analyses</span>
                        </div>
                    </div>
                    <div className={Styles.statCard}>
                        <div className={Styles.statIcon} style={{ background: "rgba(16, 185, 129, 0.15)" }}>
                            <i className="fa-solid fa-trophy" style={{ color: "#6ee7b7" }}></i>
                        </div>
                        <div className={Styles.statInfo}>
                            <span className={Styles.statNum}>{bestScore}</span>
                            <span className={Styles.statLabel}>Best Score</span>
                        </div>
                    </div>
                    <div className={Styles.statCard}>
                        <div className={Styles.statIcon} style={{ background: "rgba(59, 130, 246, 0.15)" }}>
                            <i className="fa-solid fa-chart-simple" style={{ color: "#93c5fd" }}></i>
                        </div>
                        <div className={Styles.statInfo}>
                            <span className={Styles.statNum}>{avgScore}</span>
                            <span className={Styles.statLabel}>Avg Score</span>
                        </div>
                    </div>
                </div>

                {/* Score Trend Chart */}
                {chartData.length > 1 && (
                    <div className={Styles.chartSection}>
                        <h2 className={Styles.sectionTitle}>
                            <i className="fa-solid fa-chart-line"></i> Score Trends
                        </h2>
                        <div className={Styles.chartContainer}>
                            <ResponsiveContainer width="100%" height={300}>
                                <LineChart data={chartData}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                                    <XAxis dataKey="date" stroke="#6b7280" fontSize={12} />
                                    <YAxis domain={[0, 100]} stroke="#6b7280" fontSize={12} />
                                    <Tooltip
                                        contentStyle={{ background: "#1a1a1a", border: "1px solid rgba(255,255,255,0.1)", borderRadius: "8px", color: "#e5e7eb" }}
                                    />
                                    <Legend />
                                    <Line type="monotone" dataKey="score" name="Resume Score" stroke="#7c3aed" strokeWidth={2} dot={{ fill: "#7c3aed", r: 4 }} />
                                    <Line type="monotone" dataKey="atsScore" name="ATS Score" stroke="#ec4899" strokeWidth={2} dot={{ fill: "#ec4899", r: 4 }} />
                                </LineChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                )}

                {/* Analysis History */}
                <div className={Styles.tableSection}>
                    <h2 className={Styles.sectionTitle}>
                        <i className="fa-solid fa-clock-rotate-left"></i> Analysis History
                    </h2>
                    {analysisHistory.length === 0 ? (
                        <div className={Styles.emptyState}>
                            <i className="fa-solid fa-inbox"></i>
                            <p>No analyses yet. Upload a resume to get started.</p>
                            <button className={Styles.emptyBtn} onClick={() => navigate("/uploaddoc")}>Upload Resume</button>
                        </div>
                    ) : (
                        <div className={Styles.tableWrapper}>
                            <table className={Styles.table}>
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Role</th>
                                        <th>Score</th>
                                        <th>ATS Score</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {analysisHistory.map((r, i) => (
                                        <tr key={i}>
                                            <td>{formatDateTime(r.analyzedAt)}</td>
                                            <td>{r.roles}</td>
                                            <td><span className={Styles.scoreBadge} style={{ background: r.score >= 70 ? "rgba(16,185,129,0.15)" : r.score >= 50 ? "rgba(245,158,11,0.15)" : "rgba(239,68,68,0.15)", color: r.score >= 70 ? "#6ee7b7" : r.score >= 50 ? "#fcd34d" : "#fca5a5" }}>{r.score}</span></td>
                                            <td><span className={Styles.scoreBadge} style={{ background: r.atsoptimizationscore >= 70 ? "rgba(16,185,129,0.15)" : r.atsoptimizationscore >= 50 ? "rgba(245,158,11,0.15)" : "rgba(239,68,68,0.15)", color: r.atsoptimizationscore >= 70 ? "#6ee7b7" : r.atsoptimizationscore >= 50 ? "#fcd34d" : "#fca5a5" }}>{r.atsoptimizationscore}</span></td>
                                            <td>
                                                <button className={Styles.actionBtn} onClick={() => handleDownloadPdf(r.id)} title="Download PDF">
                                                    <i className="fa-solid fa-file-pdf"></i>
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>

            </div>

            <div className={LoadStyles.loadani} id="animate">
                <div className={LoadStyles.loadanimation}>
                    <div className={LoadStyles.capstart}></div>
                    <div className={LoadStyles.loadblock}></div>
                </div>
                <h1>ResumeIQ</h1>
            </div>

            <Footer />
        </div>
    )
}

export default Dashboard
