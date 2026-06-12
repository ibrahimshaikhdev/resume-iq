import { useContext, useState, useEffect } from "react";
import Styles from "./login.module.css";
import { toast } from "react-toastify";
import { usercontext } from "../appcontext";
import { useNavigate } from "react-router-dom";
import Footer from "../components/footer.jsx"

function Login() {
    const navigate = useNavigate()
    const [islogin, setislogin] = useState(true)
    const { backendURL, serviceURL, setisprevious, setusername, setislogged, islogged } = useContext(usercontext)
    const [email, setemail] = useState("")
    const [password, setpassword] = useState("")
    const [confirmpassword, setconfirmpassword] = useState("")
    const [isloading, setisloading] = useState(false)
    const [showpass, setshowpass] = useState(false)
    const [showconfirmpass, setshowconfirmpass] = useState(false)

    useEffect(() => {
        if (islogged) {
            navigate("/")
        }
    }, [islogged])

    function validateEmail(email) {
        const emailregex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailregex.test(email)
    }

    const submit = (event) => {
        event.preventDefault()

        if (!islogin) {
            // Signup
            if (email.trim() === "") {
                toast.warn("Email must not be empty")
                return;
            }
            if (!validateEmail(email.trim())) {
                toast.warn("Invalid Email")
                return;
            }
            if (password.length < 6) {
                toast.warn("Password must have at least 6 characters")
                return;
            }
            if (password !== confirmpassword) {
                toast.warn("Passwords don't match")
                return;
            }

            setisloading(true)
            fetch(`${backendURL}/register`, {
                method: "post",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email.trim(), password: password })
            })
                .then(response => {
                    setisloading(false)
                    if (response.ok) {
                        toast.success("Account created! Please sign in.")
                        setemail("")
                        setpassword("")
                        setconfirmpassword("")
                        setislogin(true)
                    } else {
                        return response.text().then(text => {
                            let message = "Registration failed"
                            try {
                                const parsed = JSON.parse(text)
                                message = parsed.message || message
                            } catch {
                                if (text) message = text
                            }
                            if (response.status === 409) {
                                // Email already registered — guide user to sign in
                                toast.info(message)
                                setpassword("")
                                setconfirmpassword("")
                                setislogin(true)
                            } else {
                                toast.error(message)
                            }
                        })
                    }
                })
                .catch(() => {
                    setisloading(false)
                    toast.error("Signup Failed")
                })

        } else {
            // Login
            if (email.trim() === "") {
                toast.warn("Email must not be empty")
                return;
            }
            if (!validateEmail(email.trim())) {
                toast.warn("Invalid Email")
                return;
            }
            if (password.length < 6) {
                toast.warn("Password must have at least 6 characters")
                return;
            }

            setisloading(true)
            fetch(`${backendURL}/login`, {
                method: "post",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email.trim(), password: password }),
                credentials: 'include'
            })
                .then(response => {
                    if (response.ok) {
                        return response.json()
                    } else {
                        setisloading(false)
                        toast.error("Invalid email or password")
                        return null
                    }
                })
                .then(data => {
                    if (data != null) {
                        setislogged(true)
                        setusername(data.username)
                        setisprevious(data.isPrevious)
                        setisloading(false)
                        toast.success("Welcome back!")

                        // Claim anonymous results if any
                        const pendingHash = localStorage.getItem("pendingContentHash")
                        if (pendingHash) {
                            fetch(`${serviceURL}/claimAnonymous`, {
                                method: "post",
                                headers: { "Content-Type": "application/json" },
                                credentials: "include",
                                body: JSON.stringify({ contentHash: pendingHash })
                            }).then(r => r.ok ? r.json() : null).then(result => {
                                if (result && result.claimed) {
                                    localStorage.removeItem("pendingContentHash")
                                    localStorage.removeItem("freeAtsResult")
                                    setisprevious(true)
                                    toast.success("Your previous analysis has been saved!")
                                }
                                navigate("/")
                            }).catch(() => navigate("/"))
                        } else {
                            navigate("/")
                        }
                    }
                })
                .catch(() => {
                    setisloading(false)
                    toast.error("Login Failed")
                })
        }
    }

    function switchmth() {
        setemail("")
        setpassword("")
        setshowpass(false)
        setshowconfirmpass(false)
        setconfirmpassword("")
        setislogin(!islogin)
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <h1 className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</h1>
            </nav>

            <div className={Styles.card}>
                <h1 className={Styles.cardTitle}>{islogin ? "Welcome back" : "Create account"}</h1>
                <p className={Styles.cardSubtitle}>{islogin ? "Sign in to your ResumeIQ account" : "Get started with ResumeIQ"}</p>

                <input type="email" className={Styles.input} onChange={(event) => setemail(event.target.value)} name="email" id="email" value={email} autoComplete="off" placeholder="Email address" />
                <div className={Styles.passdiv}>
                    <input type={`${showpass ? "text" : "password"}`} onChange={(event) => setpassword(event.target.value)} name="password" id="password" value={password} autoComplete="off" placeholder="Password" />
                    <i className={`fa-solid ${showpass ? "fa-eye-slash" : "fa-eye"}`} onClick={() => setshowpass(!showpass)}></i>
                </div>
                {!islogin && (
                    <div className={Styles.passdiv}>
                        <input type={`${showconfirmpass ? "text" : "password"}`} name="confirmpassword" id="confirmpassword" onChange={(event) => setconfirmpassword(event.target.value)} placeholder="Confirm password" autoComplete="off" value={confirmpassword} />
                        <i className={`fa-solid ${showconfirmpass ? "fa-eye-slash" : "fa-eye"}`} onClick={() => setshowconfirmpass(!showconfirmpass)}></i>
                    </div>
                )}
                <button className={Styles.submitBtn} onClick={submit} disabled={isloading}>{isloading ? "Loading..." : islogin ? "Sign In" : "Create Account"}</button>
                <p className={Styles.switchText}>{islogin ? "Don't have an account? " : "Already have an account? "} <span className={Styles.switchLink} onClick={switchmth}>{islogin ? "Sign up" : "Sign in"}</span></p>
            </div>

            <Footer />
        </div>
    )
}

export default Login
