import { useContext, useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { usercontext } from "../appcontext"
import Styles from "./AdminDashboard.module.css"
import Footer from "../components/footer.jsx"

function AdminDashboard() {
    const { serviceURL } = useContext(usercontext)
    const navigate = useNavigate()
    const [health, setHealth] = useState(null)
    const [metrics, setMetrics] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        loadData()
        const interval = setInterval(loadData, 30000) // Refresh every 30s
        return () => clearInterval(interval)
    }, [])

    const loadData = () => {
        Promise.all([
            fetch(`${serviceURL}/health`, { credentials: "include" }).then(r => r.json()),
            fetch(`${serviceURL}/metrics`, { credentials: "include" }).then(r => r.json())
        ]).then(([h, m]) => {
            setHealth(h)
            setMetrics(m)
            setLoading(false)
        }).catch(() => setLoading(false))
    }

    if (loading) {
        return (
            <div className={Styles.container}>
                <div className={Styles.loading}>
                    <i className="fa-solid fa-spinner fa-spin-pulse"></i>
                    <span>Loading monitoring data...</span>
                </div>
            </div>
        )
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <span className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</span>
                <div className={Styles.navRight}>
                    <span className={Styles.badge}>
                        <i className="fa-solid fa-shield-halved"></i> Admin
                    </span>
                    <button className={Styles.navbtn} onClick={() => navigate("/")}>
                        <i className="fa-solid fa-arrow-left"></i> Home
                    </button>
                </div>
            </nav>

            <div className={Styles.content}>
                <h1 className={Styles.title}>
                    <i className="fa-solid fa-chart-line"></i> System Monitoring
                </h1>

                {/* Health Status */}
                <div className={Styles.section}>
                    <h2><i className="fa-solid fa-heart-pulse"></i> Health Status</h2>
                    <div className={Styles.healthGrid}>
                        <div className={`${Styles.healthCard} ${health?.status === "UP" ? Styles.healthUp : Styles.healthDown}`}>
                            <div className={Styles.healthIcon}>
                                <i className={`fa-solid ${health?.status === "UP" ? "fa-circle-check" : "fa-circle-xmark"}`}></i>
                            </div>
                            <div>
                                <h3>System</h3>
                                <span>{health?.status || "UNKNOWN"}</span>
                            </div>
                        </div>

                        <div className={`${Styles.healthCard} ${health?.database?.status === "UP" ? Styles.healthUp : Styles.healthDown}`}>
                            <div className={Styles.healthIcon}>
                                <i className={`fa-solid ${health?.database?.status === "UP" ? "fa-database" : "fa-triangle-exclamation"}`}></i>
                            </div>
                            <div>
                                <h3>Database</h3>
                                <span>{health?.database?.status || "UNKNOWN"}</span>
                            </div>
                        </div>

                        <div className={Styles.healthCard}>
                            <div className={Styles.healthIcon}>
                                <i className="fa-solid fa-memory"></i>
                            </div>
                            <div>
                                <h3>Memory</h3>
                                <span>{health?.memory?.heapUsedMb || 0} / {health?.memory?.heapMaxMb || 0} MB</span>
                                <div className={Styles.progressBar}>
                                    <div className={Styles.progressFill}
                                        style={{ width: `${health?.memory?.heapUsagePercent || 0}%` }}></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Performance Metrics */}
                <div className={Styles.section}>
                    <h2><i className="fa-solid fa-gauge-high"></i> Performance Metrics</h2>
                    <div className={Styles.metricsGrid}>
                        <div className={Styles.metricCard}>
                            <div className={Styles.metricIcon}>
                                <i className="fa-solid fa-file-lines"></i>
                            </div>
                            <div className={Styles.metricValue}>{metrics?.totalAnalyses || 0}</div>
                            <div className={Styles.metricLabel}>Total Analyses</div>
                        </div>

                        <div className={Styles.metricCard}>
                            <div className={Styles.metricIcon}>
                                <i className="fa-solid fa-clock"></i>
                            </div>
                            <div className={Styles.metricValue}>{metrics?.avgAnalysisTimeMs || 0}ms</div>
                            <div className={Styles.metricLabel}>Avg Processing Time</div>
                        </div>

                        <div className={Styles.metricCard}>
                            <div className={Styles.metricIcon}>
                                <i className="fa-solid fa-robot"></i>
                            </div>
                            <div className={Styles.metricValue}>{metrics?.totalAiCalls || 0}</div>
                            <div className={Styles.metricLabel}>AI API Calls</div>
                        </div>

                        <div className={Styles.metricCard}>
                            <div className={Styles.metricIcon}>
                                <i className="fa-solid fa-bug"></i>
                            </div>
                            <div className={Styles.metricValue}>{metrics?.aiFailures || 0}</div>
                            <div className={Styles.metricLabel}>AI Failures</div>
                        </div>

                        <div className={Styles.metricCard}>
                            <div className={Styles.metricIcon}>
                                <i className="fa-solid fa-database"></i>
                            </div>
                            <div className={Styles.metricValue}>{metrics?.cacheHitRatePercent || 0}%</div>
                            <div className={Styles.metricLabel}>Cache Hit Rate</div>
                        </div>

                        <div className={Styles.metricCard}>
                            <div className={Styles.metricIcon}>
                                <i className="fa-solid fa-bolt"></i>
                            </div>
                            <div className={Styles.metricValue}>{metrics?.cacheHits || 0}</div>
                            <div className={Styles.metricLabel}>Cache Hits</div>
                        </div>
                    </div>
                </div>

                {/* Cache Performance */}
                <div className={Styles.section}>
                    <h2><i className="fa-solid fa-database"></i> Cache Performance</h2>
                    <div className={Styles.cacheBar}>
                        <div className={Styles.cacheBarInner}>
                            <div className={Styles.cacheHits}
                                style={{ width: `${metrics?.cacheHitRatePercent || 0}%` }}>
                                Hits: {metrics?.cacheHits || 0}
                            </div>
                            <div className={Styles.cacheMisses}>
                                Misses: {metrics?.cacheMisses || 0}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Endpoint Timings */}
                {metrics?.endpointTimings && Object.keys(metrics.endpointTimings).length > 0 && (
                    <div className={Styles.section}>
                        <h2><i className="fa-solid fa-stopwatch"></i> Endpoint Timings</h2>
                        <div className={Styles.timingTable}>
                            <div className={Styles.timingHeader}>
                                <span>Endpoint</span>
                                <span>Total Time (ms)</span>
                            </div>
                            {Object.entries(metrics.endpointTimings).map(([endpoint, time]) => (
                                <div key={endpoint} className={Styles.timingRow}>
                                    <span>{endpoint}</span>
                                    <span>{time}ms</span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            <Footer />
        </div>
    )
}

export default AdminDashboard
