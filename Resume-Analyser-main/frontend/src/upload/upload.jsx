import { toast } from "react-toastify"
import Styles from "./upload.module.css"
import { useContext, useState } from "react"
import { usercontext } from "../appcontext";
import { useNavigate } from "react-router-dom";
import Footer from "../components/footer.jsx"

function Uploadpage() {
    const { serviceURL, islogged } = useContext(usercontext)
    const navigate = useNavigate()
    const [filename, setfilename] = useState("No file uploaded")
    const [isSubmitting, setIsSubmitting] = useState(false)

    const validate = () => {
        var inp = document.getElementById("resume")
        var file = inp.files[0]
        if (!file) return;
        if (!['application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document'].includes(file.type)) {
            toast.error("Upload a resume in PDF/DOC format")
            inp.value = "";
            setfilename("No file uploaded")
        } else if (file.size > 2 * 1024 * 1024) {
            toast.error("Upload a file less than 2MB")
            inp.value = ""
            setfilename("No file uploaded")
        } else {
            var str = file.name;
            if (str.length <= 20) {
                setfilename(str)
            } else {
                setfilename(str.substring(0, 9) + "..." + str.substring(str.length - 7, str.length))
            }
        }
    }

    const analysedoc = (event) => {
        event.preventDefault()
        var uploadform = document.getElementById("upform")
        var formdata = new FormData(uploadform)
        // No role input — score the resume on general professional standards.
        formdata.set("roles", "General Resume Evaluation")
        if (!formdata.get("file") || !formdata.get("file").name) {
            toast.warn("Please upload the resume")
            return;
        }
        setIsSubmitting(true)

        if (islogged) {
            // Authenticated flow — async job endpoint
            fetch(`${serviceURL}/jobs/analyze`, {
                method: "post",
                body: formdata,
                credentials: "include"
            }).then(response => {
                if (response.ok) return response.json();
                else throw new Error("Upload failed");
            }).then(data => {
                setIsSubmitting(false)
                navigate(`/jobprogress?jobId=${data.jobId}&type=ANALYSIS`)
            }).catch(() => {
                setIsSubmitting(false)
                toast.error("Failed to start analysis. Please try again.")
            })
        } else {
            // Guest flow — public ATS endpoint
            fetch(`${serviceURL}/public/ats-score`, {
                method: "post",
                body: formdata
            }).then(response => {
                return response.text().then(text => {
                    if (!response.ok) {
                        try {
                            const err = JSON.parse(text);
                            throw new Error(err.error || "Analysis failed");
                        } catch (e) {
                            if (e.message && !e.message.includes("JSON")) throw e;
                            throw new Error("Server error. Please try again.");
                        }
                    }
                    if (!text) throw new Error("Empty response from server");
                    return JSON.parse(text);
                });
            }).then(data => {
                setIsSubmitting(false)
                localStorage.setItem("freeAtsResult", JSON.stringify({
                    score: data.score || 0,
                    atsScore: data.atsoptimizationscore || data.atsScore || 0,
                    atsBreakdown: data.atsbreakdown || data.atsBreakdown || {},
                    matchedSkills: data.matchedSkills || [],
                    missingSkills: data.missingSkills || [],
                    dimensionScores: data.dimensionScores || {}
                }))
                localStorage.setItem("pendingContentHash", data.contentHash)
                navigate("/free-result")
            }).catch(err => {
                setIsSubmitting(false)
                toast.error(err.message || "Failed to analyze resume. Please try again.")
            })
        }
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <span className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</span>
                <div className={Styles.navActions}>
                    {islogged ? (
                        <>
                            <button className={Styles.navbtn} onClick={() => navigate("/dashboard")}>
                                <i className="fa-solid fa-chart-line"></i> Dashboard
                            </button>
                            <button className={Styles.navbtn} onClick={() => navigate("/")}>
                                <i className="fa-solid fa-house"></i> Home
                            </button>
                        </>
                    ) : (
                        <button className={Styles.navbtn} onClick={() => navigate("/login")}>
                            <i className="fa-solid fa-right-to-bracket"></i> Sign In
                        </button>
                    )}
                </div>
            </nav>

            <div className={Styles.doc}>
                <form id="upform" className={Styles.upform} onSubmit={analysedoc}>
                    <div className={Styles.formHeader}>
                        <h1>Resume Analysis</h1>
                        <p>Upload your resume and get an instant ATS score — no sign up required.</p>
                    </div>
                    <div className={Styles.inpgp}>
                        <label className={Styles.inplabel}>
                            <i className="fa-solid fa-file-arrow-up"></i> Upload Resume
                        </label>
                        <div className={Styles.fileInput}>
                            <input type="file" name="file" id="resume" accept=".pdf,.doc,.docx" onChange={validate} />
                            <i className="fa-solid fa-cloud-arrow-up"></i>
                            <span>{filename}</span>
                        </div>
                    </div>
                    <button type="submit" className={Styles.upbtn} disabled={isSubmitting}>
                        {isSubmitting ? (
                            <><i className="fa-solid fa-spinner fa-spin"></i> Analyzing...</>
                        ) : (
                            <><i className="fa-solid fa-magnifying-glass-chart"></i> Get ATS Score</>
                        )}
                    </button>
                </form>
            </div>
            <Footer />
        </div>
    )
}

export default Uploadpage
