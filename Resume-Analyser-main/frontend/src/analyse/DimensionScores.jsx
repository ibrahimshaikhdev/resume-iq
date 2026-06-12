import Styles from "./DimensionScores.module.css"

const DIMENSION_CONFIG = {
    semanticRelevance: { label: "Semantic Relevance", icon: "fa-solid fa-bullseye", color: "#7c3aed" },
    technicalSkillCoverage: { label: "Technical Skills", icon: "fa-solid fa-code", color: "#3b82f6" },
    experienceAlignment: { label: "Experience Fit", icon: "fa-solid fa-briefcase", color: "#10b981" },
    achievementQuality: { label: "Achievements", icon: "fa-solid fa-trophy", color: "#f59e0b" },
    keywordDensity: { label: "Keywords", icon: "fa-solid fa-magnifying-glass", color: "#ec4899" },
    formattingConfidence: { label: "Formatting", icon: "fa-solid fa-file-lines", color: "#6366f1" }
}

function DimensionScores({ scores, explanations }) {
    if (!scores) return null

    const getScoreColor = (score) => {
        if (score >= 80) return "#10b981"
        if (score >= 60) return "#f59e0b"
        if (score >= 40) return "#f97316"
        return "#ef4444"
    }

    const getScoreLabel = (score) => {
        if (score >= 80) return "Strong"
        if (score >= 60) return "Moderate"
        if (score >= 40) return "Weak"
        return "Needs Work"
    }

    return (
        <div className={Styles.container}>
            <h2 className={Styles.title}>
                <i className="fa-solid fa-chart-radar"></i>
                Intelligence Analysis
            </h2>
            <p className={Styles.subtitle}>Multi-dimensional scoring across 6 key areas</p>

            <div className={Styles.grid}>
                {Object.entries(DIMENSION_CONFIG).map(([key, config]) => {
                    const score = scores[key] || 0
                    const explanation = explanations?.[key] || ""
                    const scoreColor = getScoreColor(score)

                    return (
                        <div key={key} className={Styles.dimensionCard}>
                            <div className={Styles.cardHeader}>
                                <div className={Styles.iconWrapper} style={{ background: `${config.color}20`, color: config.color }}>
                                    <i className={config.icon}></i>
                                </div>
                                <div className={Styles.headerInfo}>
                                    <h3>{config.label}</h3>
                                    <span className={Styles.scoreLabel} style={{ color: scoreColor }}>
                                        {getScoreLabel(score)}
                                    </span>
                                </div>
                                <div className={Styles.scoreCircle} style={{ borderColor: scoreColor }}>
                                    <span style={{ color: scoreColor }}>{score}</span>
                                </div>
                            </div>

                            <div className={Styles.progressContainer}>
                                <div className={Styles.progressBar}>
                                    <div
                                        className={Styles.progressFill}
                                        style={{ width: `${score}%`, background: `linear-gradient(90deg, ${config.color}, ${scoreColor})` }}
                                    ></div>
                                </div>
                            </div>

                            {explanation && (
                                <p className={Styles.explanation}>
                                    <i className="fa-solid fa-lightbulb"></i>
                                    {explanation}
                                </p>
                            )}
                        </div>
                    )
                })}
            </div>
        </div>
    )
}

export default DimensionScores
