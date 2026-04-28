exports.notFound = (req, res) => {
  res.status(404).json({ message: 'Route not found' });
};

exports.errorHandler = (err, req, res, next) => {
  // eslint-disable-next-line no-console
  console.error(err);

  if (res.headersSent) {
    return next(err);
  }

  return res.status(500).json({
    message: err.message || 'Internal Server Error'
  });
};
