import Styles from "./home.module.css";
import { useContext, useEffect, useState } from "react";
import { usercontext } from "../appcontext";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Footer from "../components/footer.jsx";

function Home() {
    const navigate = useNavigate()
    const { islogged, username, isprevious, serviceURL, setusername, setislogged, setisprevious } = useContext(usercontext)
    const [isshow, setshow] = useState(false)
    const [isloading, setisloading] = useState(false)
    const [delloading, setdelloading] = useState(false)

    useEffect(() => {
        const func = (event) => {
            if (event.target.id != "menu") {
                setshow(false)
            }
        }
        window.addEventListener("click", func)
        return () => window.removeEventListener("click", func)
    }, [])

    const logout = () => {
        setisloading(true)
        fetch(`${serviceURL}/logout`, { method: "post", credentials: 'include' }).then(response => {
            if (response.ok) {
                setusername("")
                setislogged(false)
                setisprevious(false)
                toast.success("Successfully Logged out")
                setisloading(false)
                navigate("/login")
            } else {
                toast.error("Unauthorised access")
                setisloading(false)
            }
        }).catch(error => { toast.error("Logout failed"); setisloading(false) });
    }

    function toggle() {
        setshow(!isshow)
    }

    const confirmagain = () => {
        document.getElementById("confirmdivdel").style.display = "flex"
    }

    const closedeldiv = () => {
        document.getElementById("confirmdivdel").style.display = "none"
    }

    const delaccount = () => {
        setdelloading(true)
        fetch(`${serviceURL}/deleteAccount`, { method: "delete", credentials: 'include' }).then(response => {
            if (response.ok) {
                setusername("")
                setislogged(false)
                setisprevious(false)
                toast.success("Account deleted successfully")
                setdelloading(false)
                navigate("/login")
            } else {
                toast.error("Failed to delete account")
                setdelloading(false)
            }
        }).catch(() => { toast.error("Failed to delete account"); setdelloading(false) });
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <span className={Styles.brand}>ResumeIQ</span>
                {islogged ? (
                    <div className={Styles.navright}>
                        <span className={Styles.username} id="menu" onClick={toggle}>{username}</span>
                        {isshow && (
                            <div className={Styles.profilemenu}>
                                <div className={Styles.pmenusec}>
                                    <button onClick={() => navigate("/dashboard")}><i className="fa-solid fa-chart-line"></i> Dashboard</button>
                                    <button onClick={logout} disabled={isloading}>
                                        {isloading ? <i className="fa-solid fa-spinner fa-spin"></i> : <i className="fa-solid fa-right-from-bracket"></i>} Logout
                                    </button>
                                    <button onClick={confirmagain} className={Styles.del}>
                                        <i className="fa-solid fa-trash"></i> Delete Account
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                ) : (
                    <button className={Styles.navbtn} onClick={() => navigate("/login")}>
                        <i className="fa-solid fa-right-to-bracket"></i> Sign In
                    </button>
                )}
            </nav>

            {/* Delete Confirmation */}
            <div className={Styles.delcontainer} id="confirmdivdel">
                <div className={Styles.confirmcontainer}>
                    <h3>Delete Account?</h3>
                    <p>This will permanently remove all your data and analysis history.</p>
                    <div className={Styles.confirmationbtns}>
                        <button onClick={closedeldiv} className={Styles.notnow}>Cancel</button>
                        <button onClick={delaccount} className={Styles.confirmdel} disabled={delloading}>
                            {delloading ? <i className="fa-solid fa-spinner fa-spin"></i> : "Delete"}
                        </button>
                    </div>
                </div>
            </div>

            <main className={Styles.hero}>
                <div className={Styles.heroContent}>
                    <h1 className={Styles.heroTitle}>
                        Optimize Your Resume with
                        <span> AI Intelligence</span>
                    </h1>
                    <p className={Styles.heroSubtitle}>
                        Get instant ATS scores, skill gap analysis, and actionable recommendations
                        to make your resume stand out in the job market.
                    </p>
                    <div className={Styles.heroActions}>
                        <button className={Styles.ctaBtn} onClick={() => navigate("/uploaddoc")}>
                            <i className="fa-solid fa-magnifying-glass-chart"></i>
                            {islogged ? "Analyze Resume" : "Get Free ATS Score"}
                        </button>
                        {!islogged && (
                            <button className={Styles.secondaryBtn} onClick={() => navigate("/login")}>
                                <i className="fa-solid fa-right-to-bracket"></i> Sign Up for Full Analysis
                            </button>
                        )}
                        {islogged && isprevious && (
                            <button className={Styles.secondaryBtn} onClick={() => navigate("/dashboard")}>
                                <i className="fa-solid fa-chart-line"></i> View Dashboard
                            </button>
                        )}
                    </div>

                    {!islogged && (
                        <div className={Styles.heroVisual}>
                            <div className={Styles.heroCard}>
                                <div className={Styles.cardIcon}><i className="fa-solid fa-gauge-high"></i></div>
                                <h3>Free ATS Score includes:</h3>
                                <p>Overall Score, ATS Breakdown, and more. Sign up for full detailed analysis.</p>
                                <div className={Styles.scoreBar}><div className={Styles.scoreFill}></div></div>
                            </div>
                        </div>
                    )}
                </div>

                {islogged && (
                    <div className={Styles.heroVisual}>
                        <div className={Styles.heroCard}>
                            <div className={Styles.cardIcon}><i className="fa-solid fa-brain"></i></div>
                            <h3>AI Analysis</h3>
                            <p>Comprehensive evaluation across multiple dimensions</p>
                            <div className={Styles.scoreBar}><div className={Styles.scoreFill}></div></div>
                        </div>
                        <div className={Styles.heroCard}>
                            <div className={Styles.cardIcon}><i className="fa-solid fa-chart-bar"></i></div>
                            <h3>ATS Breakdown</h3>
                            <p>Detailed sub-scores for every aspect of your resume</p>
                            <div className={Styles.scoreBar}><div className={Styles.scoreFill2}></div></div>
                        </div>
                        <div className={Styles.heroCard}>
                            <div className={Styles.cardIcon}><i className="fa-solid fa-bullseye"></i></div>
                            <h3>Skill Gap Analysis</h3>
                            <p>Identify missing skills for your target roles</p>
                            <div className={Styles.scoreBar}><div className={Styles.scoreFill3}></div></div>
                        </div>
                    </div>
                )}
            </main>

            <section className={Styles.features}>
                <h2 className={Styles.sectionTitle}>Why ResumeIQ?</h2>
                <div className={Styles.featuresGrid}>
                    <div className={Styles.featureCard}>
                        <div className={Styles.featureIcon}><i className="fa-solid fa-brain"></i></div>
                        <h3>AI-Powered Analysis</h3>
                        <p>Advanced AI evaluates your resume across multiple dimensions including ATS compatibility, keyword optimization, and role alignment.</p>
                    </div>
                    <div className={Styles.featureCard}>
                        <div className={Styles.featureIcon}><i className="fa-solid fa-chart-line"></i></div>
                        <h3>ATS Score Breakdown</h3>
                        <p>Get detailed sub-scores for keyword match, formatting, readability, section clarity, and content relevance.</p>
                    </div>
                    <div className={Styles.featureCard}>
                        <div className={Styles.featureIcon}><i className="fa-solid fa-bullseye"></i></div>
                        <h3>Skill Gap Analysis</h3>
                        <p>Identify matched and missing skills for your target roles with visual heatmaps and actionable insights.</p>
                    </div>
                    <div className={Styles.featureCard}>
                        <div className={Styles.featureIcon}><i className="fa-solid fa-rocket"></i></div>
                        <h3>Instant Results</h3>
                        <p>Get your free ATS score in seconds. Sign up for full analysis with detailed recommendations.</p>
                    </div>
                </div>
            </section>

            <Footer />
        </div>
    )
}

export default Home
