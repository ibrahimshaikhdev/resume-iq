import { useContext, useEffect, useState, useRef } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { usercontext } from "../appcontext"
import { toast } from "react-toastify"
import Styles from "./JobProgress.module.css"
import Footer from "../components/footer.jsx"

const STAGES = [
    { key: "QUEUED", label: "Queued", icon: "fa-solid fa-clock", description: "Job is waiting in queue" },
    { key: "PROCESSING", label: "Processing", icon: "fa-solid fa-gear", description: "Parsing resume document" },
    { key: "SCORING", label: "Scoring", icon: "fa-solid fa-brain", description: "AI is analyzing your resume" },
    { key: "RECOMMENDATIONS", label: "Finalizing", icon: "fa-solid fa-clipboard-check", description: "Generating recommendations" },
    { key: "COMPLETED", label: "Completed", icon: "fa-solid fa-circle-check", description: "Analysis complete!" },
]

function JobProgress() {
    const { serviceURL } = useContext(usercontext)
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const jobId = searchParams.get("jobId")
    const jobType = searchParams.get("type") || "ANALYSIS"

    const [currentStatus, setCurrentStatus] = useState("QUEUED")
    const [errorMessage, setErrorMessage] = useState("")
    const [retryCount, setRetryCount] = useState(0)
    const [elapsedTime, setElapsedTime] = useState(0)
    const pollingRef = useRef(null)
    const timerRef = useRef(null)
    const startTimeRef = useRef(Date.now())

    useEffect(() => {
        if (!jobId) {
            navigate("/uploaddoc")
            return
        }

        // Start elapsed timer
        timerRef.current = setInterval(() => {
            setElapsedTime(Math.floor((Date.now() - startTimeRef.current) / 1000))
        }, 1000)

        // Start polling
        pollJobStatus()

        return () => {
            if (pollingRef.current) clearTimeout(pollingRef.current)
            if (timerRef.current) clearInterval(timerRef.current)
        }
    }, [jobId])

    const pollJobStatus = async () => {
        try {
            const res = await fetch(`${serviceURL}/jobs/${jobId}/status`, {
                method: "GET",
                credentials: "include"
            })

            if (!res.ok) {
                throw new Error("Failed to fetch job status")
            }

            const data = await res.json()
            setCurrentStatus(data.status)

            if (data.errorMessage) {
                setErrorMessage(data.errorMessage)
            }
            if (data.retryCount) {
                setRetryCount(data.retryCount)
            }

            if (data.status === "COMPLETED") {
                // Stop polling, navigate to report
                if (pollingRef.current) clearTimeout(pollingRef.current)
                if (timerRef.current) clearInterval(timerRef.current)

                toast.success("Analysis completed!")
                setTimeout(() => {
                    if (jobType === "JD_MATCH") {
                        navigate("/jdmatch")
                    } else {
                        navigate("/analysereport")
                    }
                }, 800)
                return
            }

            if (data.status === "FAILED") {
                if (pollingRef.current) clearTimeout(pollingRef.current)
                if (timerRef.current) clearInterval(timerRef.current)
                toast.error(data.errorMessage || "Analysis failed")
                return
            }

            if (data.status === "TIMEOUT") {
                if (pollingRef.current) clearTimeout(pollingRef.current)
                if (timerRef.current) clearInterval(timerRef.current)
                toast.error("Analysis timed out. Please try again.")
                return
            }

            // Continue polling
            pollingRef.current = setTimeout(pollJobStatus, 1500)

        } catch (err) {
            // Retry polling on network error
            pollingRef.current = setTimeout(pollJobStatus, 3000)
        }
    }

    const getCurrentStageIndex = () => {
        return STAGES.findIndex(s => s.key === currentStatus)
    }

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60)
        const secs = seconds % 60
        return `${mins}:${secs.toString().padStart(2, "0")}`
    }

    const isFailed = currentStatus === "FAILED" || currentStatus === "TIMEOUT"
    const currentIndex = getCurrentStageIndex()

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <span className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</span>
                <button className={Styles.navbtn} onClick={() => navigate("/uploaddoc")}>
                    <i className="fa-solid fa-arrow-left"></i> Back
                </button>
            </nav>

            <div className={Styles.content}>
                <div className={Styles.progressCard}>
                    <h2>
                        <i className="fa-solid fa-spinner fa-spin-pulse"></i>
                        {jobType === "JD_MATCH" ? "Matching Resume with JD" : "Analyzing Resume"}
                    </h2>

                    <div className={Styles.timer}>
                        <i className="fa-solid fa-stopwatch"></i>
                        <span>Elapsed: {formatTime(elapsedTime)}</span>
                    </div>

                    {/* Stage Progress Bar */}
                    <div className={Styles.stageContainer}>
                        {STAGES.map((stage, index) => {
                            const isComplete = index < currentIndex
                            const isCurrent = index === currentIndex
                            const isPending = index > currentIndex

                            return (
                                <div key={stage.key} className={Styles.stageWrapper}>
                                    <div className={`${Styles.stage} ${isComplete ? Styles.stageComplete : ""} ${isCurrent ? Styles.stageCurrent : ""} ${isPending ? Styles.stagePending : ""}`}>
                                        <div className={Styles.stageIcon}>
                                            {isComplete ? (
                                                <i className="fa-solid fa-check"></i>
                                            ) : isCurrent && !isFailed ? (
                                                <i className={`${stage.icon} fa-beat`}></i>
                                            ) : isFailed && isCurrent ? (
                                                <i className="fa-solid fa-circle-xmark"></i>
                                            ) : (
                                                <i className={stage.icon}></i>
                                            )}
                                        </div>
                                        <div className={Styles.stageInfo}>
                                            <span className={Styles.stageLabel}>{stage.label}</span>
                                            <span className={Styles.stageDesc}>
                                                {isFailed && isCurrent ? errorMessage : stage.description}
                                            </span>
                                        </div>
                                    </div>
                                    {index < STAGES.length - 1 && (
                                        <div className={`${Styles.stageLine} ${isComplete ? Styles.stageLineComplete : ""}`}></div>
                                    )}
                                </div>
                            )
                        })}
                    </div>

                    {/* Retry Info */}
                    {retryCount > 0 && !isFailed && (
                        <div className={Styles.retryInfo}>
                            <i className="fa-solid fa-rotate-right"></i>
                            <span>Retry attempt {retryCount}</span>
                        </div>
                    )}

                    {/* Failed State */}
                    {isFailed && (
                        <div className={Styles.failedActions}>
                            <button className={Styles.retryBtn} onClick={() => navigate("/uploaddoc")}>
                                <i className="fa-solid fa-arrow-rotate-left"></i> Try Again
                            </button>
                        </div>
                    )}

                    {/* Tips while waiting */}
                    {!isFailed && (
                        <div className={Styles.tips}>
                            <p><i className="fa-solid fa-lightbulb"></i> Our AI is carefully analyzing your resume against industry ATS standards</p>
                        </div>
                    )}
                </div>
            </div>

            <Footer />
        </div>
    )
}

export default JobProgress
