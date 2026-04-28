# Hosting Setup: GitHub Pages + Railway

## Summary
Set up CI/CD and hosting for the Music App:
- **Backend** (Node.js + MySQL) → Railway
- **Frontend** (React/Vite) → GitHub Pages
- **CI/CD** → GitHub Actions (auto-deploy on push to `main`)

### [x] Step: Configure frontend for deployment
- Updated `frontend/src/api.js` to use `VITE_API_URL` env var (replaced hardcoded local IP)
- Updated `frontend/vite.config.js` to support `VITE_BASE_PATH` for GitHub Pages base path

### [x] Step: Create Railway configuration
- Created `railway.toml` with Nixpacks builder, `npm start` command, and health check on `/api`

### [x] Step: Create GitHub Actions workflows
- Created `.github/workflows/deploy-backend.yml` — deploys backend to Railway on push to `main` (paths filtered to backend files)
- Created `.github/workflows/deploy-frontend.yml` — builds Vite app and deploys to GitHub Pages on push to `main` (paths filtered to `frontend/**`)

### [x] Step: Document environment variables
- Created `frontend/.env.example` with `VITE_API_URL` and `VITE_BASE_PATH`
- Updated root `.env.example` with Railway MySQL plugin variable references
