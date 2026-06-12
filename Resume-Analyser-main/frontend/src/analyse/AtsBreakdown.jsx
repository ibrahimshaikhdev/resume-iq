import Styles from "./AtsBreakdown.module.css"

const CATEGORY_META = {
    keywordMatch: { label: "Keyword Match", icon: "fa-solid fa-magnifying-glass", max: 15 },
    formatting: { label: "Formatting", icon: "fa-solid fa-file-lines", max: 15 },
    readability: { label: "Readability", icon: "fa-solid fa-book-open", max: 15 },
    sectionClarity: { label: "Section Clarity", icon: "fa-solid fa-layer-group", max: 15 },
    contentRelevance: { label: "Content Relevance", icon: "fa-solid fa-bullseye", max: 15 },
    contactInfo: { label: "Contact Info", icon: "fa-solid fa-address-card", max: 10 },
    grammar: { label: "Grammar", icon: "fa-solid fa-spell-check", max: 15 },
}

function AtsBreakdown({ breakdown, matchedSkills, missingSkills }) {
    const totalMax = Object.values(CATEGORY_META).reduce((sum, c) => sum + c.max, 0)

    return (
        <div className={Styles.wrapper}>
            {/* ATS Sub-Scores */}
            <div className={Styles.section}>
                <h2 className={Styles.sectionTitle}>
                    <i className="fa-solid fa-chart-bar"></i> ATS Breakdown
                </h2>
                <div className={Styles.barsContainer}>
                    {Object.entries(CATEGORY_META).map(([key, meta]) => {
                        const value = breakdown?.[key] ?? 0
                        const pct = Math.round((value / meta.max) * 100)
                        return (
                            <div className={Styles.barRow} key={key}>
                                <div className={Styles.barLabel}>
                                    <i className={meta.icon}></i>
                                    <span>{meta.label}</span>
                                    <span className={Styles.barValue}>{value}/{meta.max}</span>
                                </div>
                                <div className={Styles.barTrack}>
                                    <div className={Styles.barFill} style={{ width: `${pct}%` }}></div>
                                </div>
                            </div>
                        )
                    })}
                </div>
            </div>

            {/* Skill Gap Heatmap */}
            <div className={Styles.heatmapSection}>
                <h2 className={Styles.sectionTitle}>
                    <i className="fa-solid fa-table-cells"></i> Skill Gap Heatmap
                </h2>
                <div className={Styles.heatmapGrid}>
                    {matchedSkills && matchedSkills.length > 0 && matchedSkills.map((skill, i) => (
                        <div className={Styles.heatmapCell} key={`m-${i}`} style={{ background: "rgba(34, 197, 94, 0.15)", borderColor: "rgba(34, 197, 94, 0.4)" }}>
                            <i className="fa-solid fa-check" style={{ color: "#22c55e" }}></i>
                            <span>{skill}</span>
                        </div>
                    ))}
                    {missingSkills && missingSkills.length > 0 && missingSkills.map((skill, i) => (
                        <div className={Styles.heatmapCell} key={`x-${i}`} style={{ background: "rgba(239, 68, 68, 0.15)", borderColor: "rgba(239, 68, 68, 0.4)" }}>
                            <i className="fa-solid fa-xmark" style={{ color: "#ef4444" }}></i>
                            <span>{skill}</span>
                        </div>
                    ))}
                    {(!matchedSkills || matchedSkills.length === 0) && (!missingSkills || missingSkills.length === 0) && (
                        <div className={Styles.emptyState}>
                            <i className="fa-solid fa-circle-info"></i>
                            <span>No skill data available</span>
                        </div>
                    )}
                </div>
                <div className={Styles.heatmapLegend}>
                    <div className={Styles.legendItem}>
                        <div className={Styles.legendDot} style={{ background: "#22c55e" }}></div>
                        <span>Matched ({matchedSkills?.length || 0})</span>
                    </div>
                    <div className={Styles.legendItem}>
                        <div className={Styles.legendDot} style={{ background: "#ef4444" }}></div>
                        <span>Missing ({missingSkills?.length || 0})</span>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default AtsBreakdown
