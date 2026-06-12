import { useContext } from "react"
import { Navigate } from "react-router-dom"
import { usercontext } from "../appcontext.jsx"

// Guards pages that require authentication. If the user isn't logged in,
// redirect them to the sign-in page instead of showing the private page.
function ProtectedRoute({ children }) {
    const { islogged } = useContext(usercontext)

    if (!islogged) {
        return <Navigate to="/login" replace />
    }
    return children
}

export default ProtectedRoute
