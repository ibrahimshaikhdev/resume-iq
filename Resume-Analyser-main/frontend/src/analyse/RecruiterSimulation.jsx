import { useContext, useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { usercontext } from "../appcontext"
import Styles from "./RecruiterSimulation.module.css"
import Footer from "../components/footer.jsx"

const REVIEWER_CONFIG = {
    startup: {
        label: "Startup Recruiter",
        icon: "fa-solid fa-rocket",
        color: "#10b981",
        description: "Values versatility, hands-on skills, adaptability"
    },
    enterprise: {
        label: "Enterprise Recruiter",
        icon: "fa-solid fa-building",
        color: "#3b82f6",
        description: "Values structured experience, certifications, stability"
    },
    product: {
        label: "Product Company",
        icon: "fa-solid fa-cube",
        color: "#8b5cf6",
        description: "Values product thinking, metrics, user impact"
    },
    engineeringManager: {
        label: "Engineering Manager",
        icon: "fa-solid fa-users-gear",
        color: "#f59e0b",
        description: "Values technical depth, leadership, architecture"
    }
}

function RecruiterSimulation() {
    const { serviceURL } = useContext(usercontext)
    const navigate = useNavigate()
    const [simulation, setSimulation] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(false)

    useEffect(() => {
        fetch(`${serviceURL}/lastSimulation`, { credentials: "include" })
            .then(res => {
                if (res.ok) return res.json()
                throw new Error("No simulation found")
            })
            .then(data => {
                setSimulation(data)
                setLoading(false)
            })
            .catch(() => {
                setError(true)
                setLoading(false)
            })
    }, [serviceURL])

    const getScoreColor = (score) => {
        if (score >= 80) return "#10b981"
        if (score >= 60) return "#f59e0b"
        if (score >= 40) return "#f97316"
        return "#ef4444"
    }

    if (loading) {
        return (
            <div className={Styles.container}>
                <div className={Styles.loading}>
                    <i className="fa-solid fa-spinner fa-spin-pulse"></i>
                    <span>Loading simulation...</span>
                </div>
            </div>
        )
    }

    if (error || !simulation) {
        return (
            <div className={Styles.container}>
                <nav className={Styles.nav}>
                    <span className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</span>
                    <button className={Styles.navbtn} onClick={() => navigate("/uploaddoc")}>
                        <i className="fa-solid fa-arrow-left"></i> Back
                    </button>
                </nav>
                <div className={Styles.errorState}>
                    <i className="fa-solid fa-circle-exclamation"></i>
                    <h2>No Simulation Available</h2>
                    <p>Run an analysis first to see how different recruiters would evaluate your resume.</p>
                    <button className={Styles.ctaBtn} onClick={() => navigate("/uploaddoc")}>
                        <i className="fa-solid fa-upload"></i> Upload Resume
                    </button>
                </div>
                <Footer />
            </div>
        )
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <span className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</span>
                <div className={Styles.navButtons}>
                    <button className={Styles.navbtn} onClick={() => navigate("/analysereport")}>
                        <i className="fa-solid fa-chart-bar"></i> Report
                    </button>
                    <button className={Styles.navbtn} onClick={() => navigate("/")}>
                        <i className="fa-solid fa-arrow-left"></i> Home
                    </button>
                </div>
            </nav>

            <div className={Styles.content}>
                <div className={Styles.header}>
                    <h1 className={Styles.title}>
                        <i className="fa-solid fa-users"></i>
                        Recruiter Simulation
                    </h1>
                    <p className={Styles.subtitle}>
                        How 4 different reviewers would score your resume
                    </p>
                </div>

                {/* Reviewer Cards */}
                <div className={Styles.reviewerGrid}>
                    {Object.entries(REVIEWER_CONFIG).map(([key, config]) => {
                        const reviewer = simulation[key]
                        if (!reviewer) return null

                        return (
                            <div key={key} className={Styles.reviewerCard}>
                                <div className={Styles.cardHeader}>
                                    <div className={Styles.iconWrapper} style={{ background: `${config.color}20`, color: config.color }}>
                                        <i className={config.icon}></i>
                                    </div>
                                    <div className={Styles.headerInfo}>
                                        <h3>{config.label}</h3>
                                        <span className={Styles.description}>{config.description}</span>
                                    </div>
                                    <div className={Styles.scoreCircle} style={{ borderColor: getScoreColor(reviewer.score) }}>
                                        <span style={{ color: getScoreColor(reviewer.score) }}>{reviewer.score}</span>
                                    </div>
                                </div>

                                <div className={Styles.feedbackSection}>
                                    <div className={Styles.feedbackItem}>
                                        <h4><i className="fa-solid fa-comment"></i> What they would say</h4>
                                        <p>{reviewer.feedback}</p>
                                    </div>
                                    <div className={Styles.feedbackItem}>
                                        <h4><i className="fa-solid fa-thought-bubble"></i> What they think</h4>
                                        <p className={Styles.perspective}>{reviewer.perspective}</p>
                                    </div>
                                </div>
                            </div>
                        )
                    })}
                </div>

                {/* Common Assessment */}
                <div className={Styles.commonSection}>
                    <h2 className={Styles.sectionTitle}>
                        <i className="fa-solid fa-arrows-down-to-line"></i>
                        Universal Assessment
                    </h2>
                    <p className={Styles.sectionSubtitle}>What ALL reviewers agree on</p>

                    <div className={Styles.commonGrid}>
                        {simulation.commonStrengths && simulation.commonStrengths.length > 0 && (
                            <div className={Styles.commonCard}>
                                <h3><i className="fa-solid fa-circle-check"></i> Common Strengths</h3>
                                <ul>
                                    {simulation.commonStrengths.map((s, i) => (
                                        <li key={i}>{s}</li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        {simulation.commonWeaknesses && simulation.commonWeaknesses.length > 0 && (
                            <div className={Styles.commonCard}>
                                <h3><i className="fa-solid fa-circle-exclamation"></i> Common Weaknesses</h3>
                                <ul>
                                    {simulation.commonWeaknesses.map((w, i) => (
                                        <li key={i}>{w}</li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                </div>

                {/* Score Comparison */}
                <div className={Styles.comparisonSection}>
                    <h2 className={Styles.sectionTitle}>
                        <i className="fa-solid fa-chart-bar"></i>
                        Score Comparison
                    </h2>
                    <div className={Styles.comparisonBars}>
                        {Object.entries(REVIEWER_CONFIG).map(([key, config]) => {
                            const reviewer = simulation[key]
                            if (!reviewer) return null

                            return (
                                <div key={key} className={Styles.comparisonRow}>
                                    <span className={Styles.comparisonLabel}>{config.label}</span>
                                    <div className={Styles.comparisonBar}>
                                        <div
                                            className={Styles.comparisonFill}
                                            style={{ width: `${reviewer.score}%`, background: config.color }}
                                        ></div>
                                    </div>
                                    <span className={Styles.comparisonScore}>{reviewer.score}</span>
                                </div>
                            )
                        })}
                    </div>
                </div>
            </div>

            <Footer />
        </div>
    )
}

export default RecruiterSimulation
