import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { Heat } from "@alptugidin/react-circular-progress-bar"
import Styles from "./FreeAtsResult.module.css"
import AtsBreakdown from "./AtsBreakdown.jsx"
import SkillGapHeatmap from "./SkillGapHeatmap.jsx"
import Footer from "../components/footer.jsx"

function FreeAtsResult() {
    const navigate = useNavigate()
    const [score, setScore] = useState(0)
    const [atsScore, setAtsScore] = useState(0)
    const [breakdown, setBreakdown] = useState(null)
    const [matchedSkills, setMatchedSkills] = useState([])
    const [missingSkills, setMissingSkills] = useState([])
    const [dimensionScores, setDimensionScores] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const data = localStorage.getItem("freeAtsResult")
        if (!data) {
            navigate("/uploaddoc")
            return
        }
        try {
            const parsed = JSON.parse(data)
            setScore(parsed.score || 0)
            setAtsScore(parsed.atsScore || 0)
            setBreakdown(parsed.atsBreakdown || null)
            setMatchedSkills(parsed.matchedSkills || [])
            setMissingSkills(parsed.missingSkills || [])
            setDimensionScores(parsed.dimensionScores || null)
        } catch {
            navigate("/uploaddoc")
        }
        setLoading(false)
    }, [])

    if (loading) return null

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <span className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</span>
                <div className={Styles.navActions}>
                    <button className={Styles.navbtn} onClick={() => navigate("/uploaddoc")}>
                        <i className="fa-solid fa-arrow-left"></i> Try Another
                    </button>
                </div>
            </nav>

            <div className={Styles.doc}>
                {/* Score Section */}
                <div className={Styles.report}>
                    <div className={Styles.scoreCard}>
                        <Heat
                            progress={score}
                            range={{ from: 0, to: 100 }}
                            sign={{ value: '', position: 'end' }}
                            showValue={true}
                            revertBackground={true}
                            text={'Overall Score'}
                            sx={{
                                barWidth: 7,
                                bgColor: '#1a1a1a',
                                bgStrokeColor: '#333',
                                valueSize: 13,
                                textSize: 10,
                                valueFamily: 'Inter, sans-serif',
                                textFamily: 'Inter, sans-serif',
                                valueWeight: 'normal',
                                textWeight: 'normal',
                                textColor: '#ffffff',
                                valueColor: '#ffffff',
                                loadingTime: 1000,
                                strokeLinecap: 'round',
                                valueAnimation: true,
                            }}
                        />
                    </div>
                    <div className={Styles.scoreCard}>
                        <Heat
                            progress={atsScore}
                            range={{ from: 0, to: 100 }}
                            sign={{ value: '', position: 'end' }}
                            showValue={true}
                            revertBackground={true}
                            text={'ATS Score'}
                            sx={{
                                barWidth: 7,
                                bgColor: '#1a1a1a',
                                bgStrokeColor: '#333',
                                valueSize: 13,
                                textSize: 10,
                                valueFamily: 'Inter, sans-serif',
                                textFamily: 'Inter, sans-serif',
                                valueWeight: 'normal',
                                textWeight: 'normal',
                                textColor: '#ffffff',
                                valueColor: '#ffffff',
                                loadingTime: 1000,
                                strokeLinecap: 'round',
                                valueAnimation: true,
                            }}
                        />
                    </div>
                </div>

                {/* ATS Breakdown */}
                {breakdown && (
                    <div className={Styles.breakdownSection}>
                        <AtsBreakdown breakdown={breakdown} matchedSkills={matchedSkills} missingSkills={missingSkills} />
                    </div>
                )}

                {/* Skill Gap Heatmap */}
                {(matchedSkills.length > 0 || missingSkills.length > 0) && (
                    <div className={Styles.breakdownSection}>
                        <SkillGapHeatmap matchedSkills={matchedSkills} missingSkills={missingSkills} />
                    </div>
                )}

                {/* Upsell CTA */}
                <div className={Styles.upsellCard}>
                    <div className={Styles.upsellIcon}>
                        <i className="fa-solid fa-lock"></i>
                    </div>
                    <h3>Unlock Full Detailed Analysis</h3>
                    <p>Sign up to access:</p>
                    <ul className={Styles.featureList}>
                        <li><i className="fa-solid fa-check"></i> Detailed Pros, Cons & Suggestions</li>
                        <li><i className="fa-solid fa-check"></i> Multi-Dimensional Scoring</li>
                        <li><i className="fa-solid fa-check"></i> Job Recommendations</li>
                        <li><i className="fa-solid fa-check"></i> Analysis History Dashboard</li>
                        <li><i className="fa-solid fa-check"></i> PDF Report Export</li>
                    </ul>
                    <button className={Styles.signupBtn} onClick={() => navigate("/login")}>
                        <i className="fa-solid fa-arrow-right"></i> Sign Up / Login
                    </button>
                    <p className={Styles.note}>Your resume and score will be saved to your account automatically</p>
                </div>
            </div>

            <Footer />
        </div>
    )
}

export default FreeAtsResult
