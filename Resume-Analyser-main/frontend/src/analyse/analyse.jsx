import { useContext, useEffect, useState } from "react"
import Styles from "./analyse.module.css"
import LoadStyles from "../loadstyle.module.css"
import { Heat } from "@alptugidin/react-circular-progress-bar"
import { usercontext } from "../appcontext"
import { useNavigate } from "react-router-dom"
import Footer from "../components/footer.jsx"
import AtsBreakdown from "./AtsBreakdown.jsx"
import SkillGapHeatmap from "./SkillGapHeatmap.jsx"
import DimensionScores from "./DimensionScores.jsx"

function Analyse() {
    const navigate = useNavigate()
    const [score, setscore] = useState(0)
    const [atsscore, setatsscore] = useState(0)
    const [pros, setpros] = useState([])
    const [cons, setcons] = useState([])
    const [sug, setsug] = useState([])
    const [jobs, setjobs] = useState([])
    const [breakdown, setbreakdown] = useState(null)
    const [matchedSkills, setmatchedSkills] = useState([])
    const [missingSkills, setmissingSkills] = useState([])
    const [dimensionScores, setdimensionScores] = useState(null)
    const [dimensionExplanations, setdimensionExplanations] = useState(null)
    const { serviceURL } = useContext(usercontext)
    const [isfetched, setisfetched] = useState(false)
    const [iserror, setiserror] = useState(false)

    useEffect(() => {
        document.getElementById("animate").style.display = "flex";
        fetch(`${serviceURL}/lastReport`, { credentials: "include" }).then(
            response => {
                if (response.ok) {
                    return response.json()
                } else {
                    setiserror(true)
                    document.getElementById("animate").style.display = "none";
                }
            }
        ).then(data => {
            if (data != null) {
                setscore(data.score)
                setatsscore(data.atsoptimizationscore)
                setpros(data.pros)
                setcons(data.cons)
                setsug(data.suggestions)
                setjobs(data.jobs)
                setbreakdown(data.atsBreakdown || data.atsbreakdown || null)
                setdimensionScores(data.dimensionScores || null)
                setdimensionExplanations(data.dimensionExplanations || null)
                setmatchedSkills(data.matchedSkills || [])
                setmissingSkills(data.missingSkills || [])
                setisfetched(true)
                document.getElementById("animate").style.display = "none";
            }
        })
            .catch(error => {
                console.log(error)
                setiserror(true)
                document.getElementById("animate").style.display = "none";
            })
    }, [])

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <h1 className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</h1>
                <div style={{ display: "flex", gap: "8px" }}>
                    <button className={Styles.navbtn} onClick={() => navigate("/dashboard")}>Dashboard</button>
                    <button className={Styles.navbtn} onClick={() => navigate("/uploaddoc")}>Analyse</button>
                </div>
            </nav>

            {/* Loading Overlay */}
            <div className={LoadStyles.loadani} id="animate">
                <div className={LoadStyles.loadanimation}>
                    <div className={LoadStyles.capstart}></div>
                    <div className={LoadStyles.loadblock}></div>
                </div>
                <h1>Preparing Report</h1>
            </div>

            {isfetched ? (
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
                                    valueFamily: 'Poppins',
                                    textFamily: 'Poppins',
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
                                progress={atsscore}
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
                                    textSize: 8,
                                    valueFamily: 'Poppins',
                                    textFamily: 'Poppins',
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

                    {/* ATS Breakdown + Skills */}
                    {breakdown && (
                        <AtsBreakdown
                            breakdown={breakdown}
                            matchedSkills={matchedSkills}
                            missingSkills={missingSkills}
                        />
                    )}

                    {/* Phase 8: Intelligence Engine v2 - Dimension Scores */}
                    {dimensionScores && (
                        <DimensionScores
                            scores={dimensionScores}
                            explanations={dimensionExplanations}
                        />
                    )}

                    {/* Skill Gap Heatmap */}
                    {(matchedSkills.length > 0 || missingSkills.length > 0) && (
                        <SkillGapHeatmap
                            matchedSkills={matchedSkills}
                            missingSkills={missingSkills}
                        />
                    )}

                    {/* Review Sections */}
                    <div className={Styles.rev}>
                        <div className={Styles.pros}>
                            <h2><i className="fa-solid fa-circle-check"></i> Strengths</h2>
                            <ul>
                                {pros.map((item, index) => <li key={index}>{item}</li>)}
                            </ul>
                        </div>
                        <div className={Styles.cons}>
                            <h2><i className="fa-solid fa-circle-exclamation"></i> Improvements</h2>
                            <ul>
                                {cons.map((item, index) => <li key={index}>{item}</li>)}
                            </ul>
                        </div>
                        <div className={Styles.sug}>
                            <h2><i className="fa-solid fa-lightbulb"></i> Tips to Enhance</h2>
                            <ul>
                                {sug.map((item, index) => <li key={index}>{item}</li>)}
                            </ul>
                        </div>
                        {jobs.length > 0 ? (
                            <div className={Styles.jobs}>
                                <h2><i className="fa-solid fa-briefcase"></i> Suggested Jobs</h2>
                                {jobs.map((item, index) => (
                                    <div className={Styles.jobidiv} key={index}>
                                        <h3 className={Styles.jobtitle}>{item.title}</h3>
                                        <div className={Styles.jobmeta}>
                                            <span className={Styles.com}><i className="fa-solid fa-building"></i> {item.company?.display_name?.trim() || "Not specified"}</span>
                                            <span className={Styles.loc}><i className="fa-solid fa-location-dot"></i> {item.location?.display_name?.trim() || "Not specified"}</span>
                                            <span className={Styles.cat}><i className="fa-solid fa-tag"></i> {item.category?.label?.trim() || "Not specified"}</span>
                                        </div>
                                        <p className={Styles.jobdes}>{item.description}</p>
                                        <a className={Styles.joblink} href={item.redirect_url} target="_blank" rel="noreferrer">Apply Now <i className="fa-solid fa-arrow-up-right-from-square"></i></a>
                                    </div>
                                ))}
                            </div>
                        ) : null}
                    </div>
                    <Footer />
                </div>
            ) : iserror ? (
                <div className={Styles.errorState}>
                    <i className="fa-solid fa-circle-exclamation" style={{ fontSize: "48px", color: "#ef4444", marginBottom: "16px" }}></i>
                    <h1>Something went wrong</h1>
                    <p>Please try again after some time.</p>
                    <button className={Styles.retryBtn} onClick={() => navigate("/uploaddoc")}>Try Again</button>
                </div>
            ) : null}
        </div>
    )
}

export default Analyse
