import { useContext, useEffect, useState } from "react"
import Styles from "./JdMatch.module.css"
import LoadStyles from "../loadstyle.module.css"
import { usercontext } from "../appcontext"
import { useNavigate } from "react-router-dom"
import Footer from "../components/footer.jsx"
import SkillGapHeatmap from "./SkillGapHeatmap.jsx"

const CATEGORY_META = {
    technicalSkills: { label: "Technical Skills", icon: "fa-solid fa-code", max: 25 },
    experience: { label: "Experience", icon: "fa-solid fa-briefcase", max: 20 },
    education: { label: "Education", icon: "fa-solid fa-graduation-cap", max: 15 },
    keywords: { label: "Keywords", icon: "fa-solid fa-magnifying-glass", max: 15 },
    softSkills: { label: "Soft Skills", icon: "fa-solid fa-users", max: 10 },
    certifications: { label: "Certifications", icon: "fa-solid fa-certificate", max: 10 },
    projects: { label: "Projects", icon: "fa-solid fa-diagram-project", max: 5 },
}

function JdMatch() {
    const navigate = useNavigate()
    const { serviceURL } = useContext(usercontext)
    const [data, setData] = useState(null)
    const [isfetched, setisfetched] = useState(false)
    const [iserror, setiserror] = useState(false)

    useEffect(() => {
        document.getElementById("animate").style.display = "flex"
        fetch(`${serviceURL}/lastJdMatch`, { credentials: "include" })
            .then(response => {
                if (response.ok) return response.json()
                setiserror(true)
                document.getElementById("animate").style.display = "none"
            })
            .then(result => {
                if (result) {
                    setData(result)
                    setisfetched(true)
                    document.getElementById("animate").style.display = "none"
                }
            })
            .catch(() => {
                setiserror(true)
                document.getElementById("animate").style.display = "none"
            })
    }, [])

    const getScoreColor = (pct) => {
        if (pct >= 75) return "#10b981"
        if (pct >= 50) return "#f59e0b"
        if (pct >= 25) return "#ef4444"
        return "#dc2626"
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <h1 className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</h1>
                <div style={{ display: "flex", gap: "8px" }}>
                    <button className={Styles.navbtn} onClick={() => navigate("/dashboard")}>Dashboard</button>
                    <button className={Styles.navbtn} onClick={() => navigate("/uploaddoc")}>Analyse</button>
                </div>
            </nav>

            <div className={LoadStyles.loadani} id="animate">
                <div className={LoadStyles.loadanimation}>
                    <div className={LoadStyles.capstart}></div>
                    <div className={LoadStyles.loadblock}></div>
                </div>
                <h1>Matching Resume</h1>
            </div>

            {isfetched && data ? (
                <div className={Styles.doc}>
                    {/* Match Score Hero */}
                    <div className={Styles.scoreHero}>
                        <div className={Styles.scoreCircle} style={{ borderColor: getScoreColor(data.matchPercentage) }}>
                            <span className={Styles.scoreNum} style={{ color: getScoreColor(data.matchPercentage) }}>
                                {data.matchPercentage}
                            </span>
                            <span className={Styles.scoreLabel}>Match %</span>
                        </div>
                        <div className={Styles.scoreMeta}>
                            <h2>Job Description Match</h2>
                            <p>
                                {data.matchPercentage >= 75 ? "Strong match! Your resume aligns well with this job description." :
                                 data.matchPercentage >= 50 ? "Moderate match. Some skills and qualifications need attention." :
                                 data.matchPercentage >= 25 ? "Weak match. Significant gaps to address." :
                                 "Low match. Major improvements needed for this role."}
                            </p>
                        </div>
                    </div>

                    {/* Category Breakdown */}
                    {data.categoryBreakdown && (
                        <div className={Styles.breakdownSection}>
                            <h2 className={Styles.sectionTitle}>
                                <i className="fa-solid fa-chart-bar"></i> Category Breakdown
                            </h2>
                            <div className={Styles.breakdownGrid}>
                                {Object.entries(CATEGORY_META).map(([key, meta]) => {
                                    const cat = data.categoryBreakdown[key]
                                    const score = cat?.score ?? 0
                                    const pct = Math.round((score / meta.max) * 100)
                                    return (
                                        <div className={Styles.breakdownCard} key={key}>
                                            <div className={Styles.breakdownHeader}>
                                                <i className={meta.icon}></i>
                                                <span>{meta.label}</span>
                                            </div>
                                            <div className={Styles.breakdownScore}>
                                                <span className={Styles.breakdownNum}>{score}</span>
                                                <span className={Styles.breakdownMax}>/{meta.max}</span>
                                            </div>
                                            <div className={Styles.breakdownBar}>
                                                <div
                                                    className={Styles.breakdownFill}
                                                    style={{ width: `${pct}%`, background: `linear-gradient(90deg, ${getScoreColor(pct)}, ${getScoreColor(pct)}aa)` }}
                                                />
                                            </div>
                                            <span className={`${Styles.breakdownStatus} ${Styles[cat?.status || "missing"]}`}>
                                                {cat?.status || "N/A"}
                                            </span>
                                        </div>
                                    )
                                })}
                            </div>
                        </div>
                    )}

                    {/* Skill Gap Heatmap */}
                    <SkillGapHeatmap
                        matchedSkills={data.matchedSkills}
                        missingSkills={data.missingSkills}
                    />

                    {/* Strengths & Gaps */}
                    <div className={Styles.reviewGrid}>
                        <div className={Styles.reviewCard}>
                            <h2 className={Styles.strengthTitle}>
                                <i className="fa-solid fa-circle-check"></i> Strengths
                            </h2>
                            <ul>
                                {data.strengths?.map((item, i) => <li key={i}>{item}</li>)}
                            </ul>
                        </div>
                        <div className={Styles.reviewCard}>
                            <h2 className={Styles.gapTitle}>
                                <i className="fa-solid fa-circle-exclamation"></i> Gaps
                            </h2>
                            <ul>
                                {data.gaps?.map((item, i) => <li key={i}>{item}</li>)}
                            </ul>
                        </div>
                    </div>

                    {/* Recommendations */}
                    {data.recommendations?.length > 0 && (
                        <div className={Styles.recommendSection}>
                            <h2 className={Styles.recommendTitle}>
                                <i className="fa-solid fa-lightbulb"></i> Recommendations
                            </h2>
                            <ul>
                                {data.recommendations.map((item, i) => <li key={i}>{item}</li>)}
                            </ul>
                        </div>
                    )}

                    <Footer />
                </div>
            ) : iserror ? (
                <div className={Styles.errorState}>
                    <i className="fa-solid fa-circle-exclamation" style={{ fontSize: "48px", color: "#ef4444", marginBottom: "16px" }}></i>
                    <h1>No JD Match Found</h1>
                    <p>Upload a resume with a job description to see match results.</p>
                    <button className={Styles.retryBtn} onClick={() => navigate("/uploaddoc")}>Try Now</button>
                </div>
            ) : null}
        </div>
    )
}

export default JdMatch
