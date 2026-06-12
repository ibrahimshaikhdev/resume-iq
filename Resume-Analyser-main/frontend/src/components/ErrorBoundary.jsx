import { Component } from "react"

// Catches render-time errors in any child route so a crash shows a recoverable
// message instead of a silent black screen.
class ErrorBoundary extends Component {
    constructor(props) {
        super(props)
        this.state = { hasError: false, message: "" }
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, message: error?.message || "Something went wrong" }
    }

    componentDidCatch(error, info) {
        console.error("Render error caught by ErrorBoundary:", error, info)
    }

    render() {
        if (this.state.hasError) {
            return (
                <div style={{
                    minHeight: "100vh",
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    gap: "1rem",
                    background: "#0a0a0a",
                    color: "#e5e7eb",
                    fontFamily: "Inter, sans-serif",
                    textAlign: "center",
                    padding: "2rem",
                }}>
                    <h1 style={{ margin: 0 }}>ResumeIQ</h1>
                    <p style={{ color: "#9ca3af", maxWidth: 420 }}>
                        Something went wrong while rendering this page.
                    </p>
                    <code style={{ color: "#ef4444", fontSize: 13 }}>{this.state.message}</code>
                    <button
                        onClick={() => { window.location.href = "/" }}
                        style={{
                            marginTop: "0.5rem",
                            padding: "0.6rem 1.4rem",
                            borderRadius: 8,
                            border: "none",
                            background: "#7c3aed",
                            color: "#fff",
                            cursor: "pointer",
                            fontSize: 14,
                        }}
                    >
                        Back to Home
                    </button>
                </div>
            )
        }
        return this.props.children
    }
}

export default ErrorBoundary
