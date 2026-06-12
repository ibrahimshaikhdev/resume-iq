import Styles from "./SkillGapHeatmap.module.css"

const CATEGORY_ICONS = {
    "Programming Languages": "fa-solid fa-code",
    "Frameworks & Libraries": "fa-solid fa-cubes",
    "Databases": "fa-solid fa-database",
    "Cloud & DevOps": "fa-solid fa-cloud",
    "Tools & Platforms": "fa-solid fa-wrench",
    "Soft Skills": "fa-solid fa-users",
    "Certifications": "fa-solid fa-certificate",
    "Other": "fa-solid fa-tag",
}

function categorizeSkill(skill) {
    const s = skill.toLowerCase()
    if (/\b(python|java|javascript|typescript|c\+\+|c#|ruby|go|rust|php|swift|kotlin|scala|r\b|matlab|perl)\b/.test(s)) return "Programming Languages"
    if (/\b(react|angular|vue|django|flask|spring|node|express|laravel|rails|nextjs|nuxt|svelte|fastapi|tensorflow|pytorch|pandas|numpy|bootstrap|tailwind)\b/.test(s)) return "Frameworks & Libraries"
    if (/\b(mysql|postgres|mongodb|redis|sqlite|oracle|sql server|dynamodb|cassandra|firebase|elasticsearch|neo4j)\b/.test(s)) return "Databases"
    if (/\b(aws|azure|gcp|docker|kubernetes|jenkins|terraform|ci\/cd|nginx|apache|cloud|devops|ansible|prometheus|grafana)\b/.test(s)) return "Cloud & DevOps"
    if (/\b(git|github|gitlab|jira|confluence|figma|postman|linux|bash|shell|agile|scrum|junit|selenium|cypress)\b/.test(s)) return "Tools & Platforms"
    if (/\b(leadership|communication|teamwork|problem.solving|analytical|creative|time.management|adaptability|collaboration|mentoring)\b/.test(s)) return "Soft Skills"
    if (/\b(certified|certification|aws.certified|azure.certified|gcp.certified|pmp|scrum.master|cissp|comptia)\b/.test(s)) return "Certifications"
    return "Other"
}

function SkillGapHeatmap({ matchedSkills = [], missingSkills = [] }) {
    const categorized = {}

    matchedSkills.forEach(skill => {
        const cat = categorizeSkill(skill)
        if (!categorized[cat]) categorized[cat] = { matched: [], missing: [] }
        categorized[cat].matched.push(skill)
    })

    missingSkills.forEach(skill => {
        const cat = categorizeSkill(skill)
        if (!categorized[cat]) categorized[cat] = { matched: [], missing: [] }
        categorized[cat].missing.push(skill)
    })

    const categories = Object.entries(categorized).sort((a, b) => {
        const aTotal = a[1].matched.length + a[1].missing.length
        const bTotal = b[1].matched.length + b[1].missing.length
        return bTotal - aTotal
    })

    if (categories.length === 0) return null

    const totalMatched = matchedSkills.length
    const totalMissing = missingSkills.length
    const total = totalMatched + totalMissing
    const coveragePct = total > 0 ? Math.round((totalMatched / total) * 100) : 0

    return (
        <div className={Styles.wrapper}>
            <div className={Styles.header}>
                <h2 className={Styles.sectionTitle}>
                    <i className="fa-solid fa-th"></i> Skill Gap Heatmap
                </h2>
                <div className={Styles.summary}>
                    <div className={Styles.summaryItem}>
                        <span className={Styles.summaryNum}>{totalMatched}</span>
                        <span className={Styles.summaryLabel}>Matched</span>
                    </div>
                    <div className={Styles.summaryDivider} />
                    <div className={Styles.summaryItem}>
                        <span className={Styles.summaryNum}>{totalMissing}</span>
                        <span className={Styles.summaryLabel}>Missing</span>
                    </div>
                    <div className={Styles.summaryDivider} />
                    <div className={Styles.summaryItem}>
                        <span className={Styles.summaryNum}>{coveragePct}%</span>
                        <span className={Styles.summaryLabel}>Coverage</span>
                    </div>
                </div>
            </div>

            <div className={Styles.coverageBar}>
                <div className={Styles.coverageFill} style={{ width: `${coveragePct}%` }} />
            </div>

            <div className={Styles.grid}>
                {categories.map(([category, data]) => {
                    const catTotal = data.matched.length + data.missing.length
                    const catPct = Math.round((data.matched.length / catTotal) * 100)
                    const status = catPct >= 75 ? "strong" : catPct >= 50 ? "moderate" : catPct >= 25 ? "weak" : "critical"

                    return (
                        <div className={Styles.categoryCard} key={category}>
                            <div className={Styles.categoryHeader}>
                                <div className={Styles.categoryName}>
                                    <i className={CATEGORY_ICONS[category] || "fa-solid fa-tag"}></i>
                                    <span>{category}</span>
                                </div>
                                <span className={`${Styles.statusBadge} ${Styles[status]}`}>
                                    {catPct}%
                                </span>
                            </div>

                            <div className={Styles.skillCells}>
                                {data.matched.map((skill, i) => (
                                    <span className={Styles.cellMatched} key={`m-${i}`}>{skill}</span>
                                ))}
                                {data.missing.map((skill, i) => (
                                    <span className={Styles.cellMissing} key={`x-${i}`}>{skill}</span>
                                ))}
                            </div>

                            <div className={Styles.miniBar}>
                                <div
                                    className={`${Styles.miniFill} ${Styles[`fill_${status}`]}`}
                                    style={{ width: `${catPct}%` }}
                                />
                            </div>
                        </div>
                    )
                })}
            </div>
        </div>
    )
}

export default SkillGapHeatmap
