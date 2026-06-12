import { useContext, useState, useEffect } from "react"
import Styles from "./resetpassword.module.css"
import { useNavigate } from "react-router-dom"
import { usercontext } from "../appcontext"
import { toast } from "react-toastify"
import Footer from "../components/footer.jsx"

function Forgotpassword() {
    const navigate = useNavigate()
    const [email, setemail] = useState("")
    const [otp, setotp] = useState(["", "", "", "", "", ""])
    const [newpassword, setnewpassword] = useState("")
    const [confirmpassword, setconfirmpassword] = useState("")
    const [isloading, setisloading] = useState(false)
    const [isemailpresent, setisemailpresent] = useState(false)
    const [isemailverified, setisemailverified] = useState(false)
    const { backendURL, islogged } = useContext(usercontext)
    const [showpass, setshowpass] = useState(false)
    const [showconfirmpass, setshowconfirmpass] = useState(false)

    useEffect(() => {
        if (islogged) {
            navigate("/")
        }
    }, [islogged])

    const handleInput = (index, event) => {
        if (index < 5 && event.target.value != "" && event.target.value.replace(/\D/, "") != "") {
            document.getElementById(index + 1).focus()
        }
        if (event.target.value.replace(/\D/, "") != "") {
            var tem = [...otp]
            tem[index] = event.target.value
            setotp(tem)
        }
        if (event.target.value.replace(/\D/, "") == "") {
            event.target.value = ""
        }
    }

    const handlebck = (index, event) => {
        if (event.key === "Backspace") {
            if (index > 0) {
                event.target.value = ""
                document.getElementById(index - 1).focus()
                event.preventDefault()
            }
            var tem = [...otp]
            tem[index] = ""
            event.target.value = ""
            setotp(tem)
        } else {
            if (event.target.value.length === 1 && index < 5 && event.target.value.replace(/\D/, "") != "") {
                document.getElementById(index + 1).focus()
            }
        }
    }

    function validateEmail(email) {
        const emailregex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailregex.test(email)
    }

    const verifyemail = () => {
        if (email.trim() === "") {
            toast.warn("Email must not be empty")
            return;
        }
        if (!validateEmail(email.trim())) {
            toast.warn("Invalid Email")
            return;
        }
        setisloading(true)
        fetch(`${backendURL}/resetOtpSent`, { method: "post", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ "email": email }) })
            .then(response => {
                if (response.ok) {
                    toast.success("OTP sent successfully")
                    setisloading(false)
                    setisemailpresent(true)
                } else {
                    toast.error("Invalid email")
                    setisloading(false)
                }
            })
            .catch(error => {
                toast.error("Verification failed")
                setisloading(false)
            })
    }

    const verifyprocess = () => {
        var enteredOtp = ""
        otp.forEach((i) => enteredOtp += i)
        if (enteredOtp.length < 6) {
            toast.error("Fill all fields")
            return;
        }
        setisloading(true)
        fetch(`${backendURL}/verifyResetOtp`, {
            method: "post",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ "email": email, "otp": enteredOtp })
        })
            .then(response => {
                if (response.ok) {
                    toast.success("Set new password")
                    setisloading(false)
                    setisemailverified(true)
                } else {
                    setotp(["", "", "", "", "", ""])
                    toast.error("Invalid OTP")
                    setisloading(false)
                }
            })
            .catch(error => { toast.error("Verification failed"); setisloading(false) })
    }

    const resetpasswordsent = () => {
        var enteredOtp = ""
        otp.forEach((i) => enteredOtp += i)
        if (newpassword.length < 6) {
            toast.warn("Password must have at least 6 characters")
            return;
        }
        if (!(newpassword === confirmpassword)) {
            toast.warn("Passwords don't match")
            return;
        }
        setisloading(true)
        fetch(`${backendURL}/resetPassword`, { method: "post", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ "email": email, "otp": enteredOtp, "password": newpassword }) })
            .then(response => {
                if (response.ok) {
                    toast.success("Password changed successfully")
                    setisloading(false)
                    setotp(["", "", "", "", "", ""])
                    setemail("")
                    setnewpassword("")
                    setisemailverified(false)
                    setisemailpresent(false)
                    setshowpass(false)
                    setconfirmpassword("")
                    setshowconfirmpass(false)
                    navigate("/login")
                } else {
                    toast.error("Error occurred")
                    setisloading(false)
                }
            })
            .catch(error => {
                toast.error("Resetting failed")
                setisloading(false)
            })
    }

    return (
        <div className={Styles.container}>
            <nav className={Styles.nav}>
                <h1 className={Styles.brand} onClick={() => navigate("/")}>ResumeIQ</h1>
            </nav>

            {!isemailpresent && !isemailverified ? (
                <div className={Styles.card}>
                    <h1 className={Styles.cardTitle}>Reset Password</h1>
                    <p className={Styles.cardSubtitle}>Enter your email address to receive a verification OTP</p>
                    <input className={Styles.input} onChange={(event) => setemail(event.target.value)} type="email" name="email" id="email" autoComplete="off" placeholder="Email address" />
                    <button className={Styles.submitBtn} onClick={verifyemail} disabled={isloading}>{isloading ? "Sending..." : "Send OTP"}</button>
                </div>
            ) : null}

            {isemailpresent && !isemailverified ? (
                <div className={Styles.card}>
                    <h1 className={Styles.cardTitle}>Verify Email</h1>
                    <p className={Styles.cardSubtitle}>Enter the 6-digit OTP sent to your email</p>
                    <div className={Styles.otpcontainer}>
                        {otp.map((value, index) => <input inputMode="numeric" maxLength={1} placeholder="-" key={index} value={value} autoComplete="off" type="text" className={Styles.otpinp} id={index} onChange={(e) => handleInput(index, e)} onKeyDown={(e) => { handlebck(index, e) }} />)}
                    </div>
                    <button className={Styles.submitBtn} onClick={verifyprocess} disabled={isloading}>{isloading ? "Verifying..." : "Verify"}</button>
                </div>
            ) : null}

            {isemailpresent && isemailverified ? (
                <div className={Styles.card}>
                    <h1 className={Styles.cardTitle}>New Password</h1>
                    <p className={Styles.cardSubtitle}>Create a strong password for your account</p>
                    <div className={Styles.passdiv}>
                        <input onChange={(event) => setnewpassword(event.target.value)} type={`${showpass ? "text" : "password"}`} name="password" id="password" autoComplete="off" placeholder="New password" />
                        <i className={`fa-solid ${showpass ? "fa-eye-slash" : "fa-eye"}`} onClick={() => setshowpass(!showpass)}></i>
                    </div>
                    <div className={Styles.passdiv}>
                        <input type={`${showconfirmpass ? "text" : "password"}`} name="confirmpassword" id="confirmpassword" onChange={(event) => setconfirmpassword(event.target.value)} placeholder="Confirm password" autoComplete="off" value={confirmpassword} />
                        <i className={`fa-solid ${showconfirmpass ? "fa-eye-slash" : "fa-eye"}`} onClick={() => setshowconfirmpass(!showconfirmpass)}></i>
                    </div>
                    <button className={Styles.submitBtn} onClick={resetpasswordsent} disabled={isloading}>{isloading ? "Changing..." : "Change Password"}</button>
                </div>
            ) : null}

            <Footer />
        </div>
    )
}

export default Forgotpassword
