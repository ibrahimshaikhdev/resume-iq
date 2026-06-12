# ResumeIQ
[![Live App](https://img.shields.io/badge/Live-App-brightgreen)](https://resumeiq.onrender.com/)


## Description

ResumeIQ is an enterprise-grade AI-powered ATS resume intelligence platform built using React.js, Spring Boot, and MySQL.

It helps candidates optimize resumes against specific job descriptions through intelligent analysis, semantic matching, scoring, and career recommendations.

This project integrates **Google Gemini AI** for resume analysis and includes secure authentication features like email verification and password reset using **Brevo** and job suggestions using **Adzuna API**.



## Tech Stack
- Frontend: React.js, CSS Modules
- Backend: Spring Boot
- Database: MySQL



## Authors
- **Ibrahim Shaikh**
- **Vaibhav Koul**
- **Vishvesh Rajulwar**



## Frontend & Backend Integration Notes

- The frontend UI is developed using **React**
- For deployment, the React application is **built and served by the Spring Boot backend** as static files
- The React production build files are placed inside the backend's **static** directory

### Static & Template Files
- The `static` folder contains the **React production build files**
- The `templates` folder inside `static` is used to store **email templates**
  - Used for **email verification** and **password reset**


## Disclaimer
- This project is developed for learning and demonstration purposes
- AI analysis results may vary and should not be considered professional career advice
