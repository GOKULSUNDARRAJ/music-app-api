const jwt = require('jsonwebtoken');
const { User } = require('../models');

const adminUser = process.env.ADMIN_USER || 'admin';
const adminPass = process.env.ADMIN_PASS || 'admin123';
const expectedToken = Buffer.from(`${adminUser}:${adminPass}`).toString('base64');

const JWT_SECRET = process.env.JWT_SECRET || 'music_app_secret_key';

// ─── Admin Basic-Auth Middleware ──────────────────────────────────────────────
exports.requireAdminAuth = (req, res, next) => {
  const authHeader = req.headers.authorization || '';
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : '';

  if (!token || token !== expectedToken) {
    return res.status(401).json({
      status: false,
      error_type: '401',
      message: 'Unauthorized'
    });
  }

  return next();
};

// ─── User JWT Middleware ───────────────────────────────────────────────────────
exports.requireUserAuth = async (req, res, next) => {
  const authHeader = req.headers.authorization || '';

  // Accept both:  "Bearer <token>"  OR  raw "<token>"
  let token = '';
  if (authHeader.startsWith('Bearer ')) {
    token = authHeader.slice(7);
  } else if (authHeader.length > 0) {
    token = authHeader; // raw JWT sent directly
  }

  if (!token) {
    return res.status(401).json({
      status: false,
      error_type: '401',
      message: 'Access token is missing'
    });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.userId = parseInt(decoded.sub, 10);
    
    // Verify user actually exists in database to prevent foreign key errors with stale tokens
    const user = await User.findByPk(req.userId);
    if (!user) {
      return res.status(401).json({
        status: false,
        error_type: '401',
        message: 'User account no longer exists. Please log in again.'
      });
    }
    
    return next();
  } catch (err) {
    return res.status(401).json({
      status: false,
      error_type: '401',
      message: 'Invalid or expired access token'
    });
  }
};

// ─── Optional User JWT Middleware ──────────────────────────────────────────────
exports.optionalUserAuth = (req, res, next) => {
  const authHeader = req.headers.authorization || '';

  let token = '';
  if (authHeader.startsWith('Bearer ')) {
    token = authHeader.slice(7);
  } else if (authHeader.length > 0) {
    token = authHeader;
  }

  if (!token) {
    return next(); // Proceed without userId
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.userId = parseInt(decoded.sub, 10);
  } catch (err) {
    // For optional auth, we just continue without setting req.userId
  }
  
  return next();
};

