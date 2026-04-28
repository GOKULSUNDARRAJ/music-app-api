# Music App Admin Portal + API

Full-stack Music App Admin system built with Node.js, Express, MySQL, Sequelize, and React.

## Tech Stack

- Backend: Node.js, Express, Sequelize ORM, MySQL
- Frontend: React (Vite), Axios
- Features: Admin authentication, CRUD for sections/categories/songs, nested public home API

## Project Structure

- `config/` - Sequelize database connection
- `models/` - Sequelize models and associations
- `controllers/` - Request handlers and business logic
- `routes/` - API route definitions
- `middleware/` - Auth middleware
- `frontend/` - React admin dashboard
- `seed.js` - Sample data seeding script

## Database Schema

- `sections` (`id`, `sectionTitle`, `layoutType`, `spanCount`)
- `categories` (`id`, `categoryName`, `categoryImage`, `adapterType`, `sectionId`)
- `songs` (`id`, `audioName`, `audioUrl`, `imageUrl`, `categoryId`)
- `menu_items` (`id`, `menuType`, `menuName`, `menuStatusId`, `menuStatus`, `menuActiveIcon`, `menuInActiveIcon`, `sortOrder`)

Relations:

- One section has many categories
- One category has many songs

## Setup

### 1) Prerequisites

- Node.js 18+ recommended
- MySQL server running

### 2) Backend install

```bash
npm install
```

### 3) Environment variables

Copy `.env.example` to `.env` and update values:

```bash
cp .env.example .env
```

For Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

### 4) Create database

Create the database from MySQL shell or client:

```sql
CREATE DATABASE musicapp;
```

### 5) Seed sample data

```bash
npm run seed
```

### 6) Start backend

```bash
npm start
```

Backend runs at `http://localhost:3000`.

### 7) Start frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`.

## API Endpoints

### Admin auth

- `POST /api/admin/login`

Body:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Section APIs

- `POST /api/admin/section`
- `GET /api/admin/sections`
- `PUT /api/admin/section/:id`
- `DELETE /api/admin/section/:id`

### Category APIs

- `POST /api/admin/category`
- `GET /api/admin/categories?sectionId=`
- `PUT /api/admin/category/:id`
- `DELETE /api/admin/category/:id`

### Song APIs

- `POST /api/admin/song`
- `GET /api/admin/songs?categoryId=`
- `PUT /api/admin/song/:id`
- `DELETE /api/admin/song/:id`

### Menu APIs

- `POST /api/admin/menu`
- `GET /api/admin/menu?menuType=top|bottom`
- `PUT /api/admin/menu/:id`
- `DELETE /api/admin/menu/:id`

### User Management APIs (Admin Only)

- `GET /api/admin/users`
- `DELETE /api/admin/user/:id`

### User APIs

- `POST /api/user/checkRegister` (Supports `application/x-www-form-urlencoded`)
- `POST /api/user/register`
- `POST /api/user/login`
- `POST /api/user/getTermsAndConditions`
- `GET /api/user/getNavigationMenu` (Requires Auth)
- `POST /api/user/logout` (Requires Auth)
- `GET /api/user/profile` (Requires Auth)
- `PUT /api/user/profile` (Requires Auth)
- `POST /api/user/secure/numberVerification` (Requires Auth)
- `DELETE /api/user/account` (Requires Auth)

### Search API

- `GET /api/search?q=<query>`

### Home API

- `GET /api/home`
- `GET /api/devotional`
- `GET /api/menu`

Returns nested sections -> categories -> songs JSON for app home screen.

`/api/home` returns sections with `contentType=home` and `/api/devotional` returns sections with `contentType=devotional`.

## Admin Portal Features

- Login page
- Dashboard with total counts
- Sections CRUD page
- Categories CRUD page with section filter
- Songs CRUD page with category filter
- Menu Management CRUD
- User Management table (View & Delete)
- Sidebar navigation (Dashboard, Sections, Categories, Songs, Menu, Users)

## Notes

- All admin endpoints except login require `Authorization: Bearer <token>`.
- Validation ensures:
  - Category cannot be created for a non-existing section.
  - Song cannot be created for a non-existing category.
- Centralized Express error handling is enabled.
- For production, ensure `NODE_ENV=production` is set to serve the frontend.

## Deployment

### 1. Database
Host a MySQL database on a service like **Aiven**, **Railway**, or **Render**. Get your connection details.

### 2. Deployment Platforms
This project is configured for **Render** or **Railway**.

**Render Setup:**
- **Service Type:** Web Service
- **Build Command:** `npm install && npm run build`
- **Start Command:** `npm start`
- **Environment Variables:**
    - `NODE_ENV`: `production`
    - `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT`
    - `JWT_SECRET`: (Generate a random string)
    - `ADMIN_USER`, `ADMIN_PASS`: (For your admin login)

### 3. File Persistence
Most PaaS services have ephemeral storage. If you upload files to `uploads/`, they will be deleted on every redeploy. For permanent storage, consider:
- **Render:** Use a [Persistent Disk](https://render.com/docs/disks).
- **Railway:** Use [Volumes](https://docs.railway.app/reference/volumes).
- **Better Solution:** Modify the code to use **Cloudinary** or **AWS S3**.
