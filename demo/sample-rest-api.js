const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const http = require('http');

const app = express();
const PORT = process.env.PORT || 30001;

app.use(express.json());

// Ensure the uploads directory exists
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Configure multer for file uploads
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, uniqueSuffix + path.extname(file.originalname));
    }
});
const upload = multer({
    storage: storage,
    fileFilter: (req, file, cb) => {
        // Validate file type (only allow images)
        const fileTypes = /jpeg|jpg|png|gif/;
        const extname = fileTypes.test(path.extname(file.originalname).toLowerCase());
        const mimetype = fileTypes.test(file.mimetype);

        if (mimetype && extname) {
            return cb(null, true);
        } else {
            return cb(new Error('Only images are allowed!'));
        }
    }
});

// Upload Image Endpoint with Header Logging
app.post('/upload',
    upload.single('image'), (req, res) => {
    console.log('--- Upload Request Received ---');
    console.log('Headers:', req.headers); // Log request headers at start

    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }

    console.log(`Uploaded file: ${req.file.filename}`);
    console.log('Final Headers:', req.headers); // Log headers after processing

    res.status(200).json({
        message: 'File uploaded successfully',
        filename: req.file.filename,
        path: `/uploads/${req.file.filename}`
    });
});

// Route to simulate forward authentication
app.post('/verify', (req, res) => {
    console.log('--- Verify Request ---');
    console.log('Headers:', req.headers);

    const authHeader = req.headers['authorization'];
    if (!authHeader) {
        return res.status(401).json({ error: 'Authorization header missing' });
    }

    res.status(200).json({
        message: 'Authentication successful',
        user: {
            id: 'user123',
            name: 'Mock User',
            roles: ['roleA', 'roleB'],
        }
    });
});

// Fetch user details
app.get('/user', (req, res) =>{
    console.log('--- User Request ---');
    console.log('Headers:', req.headers);

    res.setHeader('X-Custom-Header', 'CustomValue');
    res.setHeader('X-Powered-By', 'Express with Love');
    res.status(200).json({
        user: {
            id: 'user123',
            name: 'Mock User',
            roles: ['roleA', 'roleB'],
        }
    });
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.status(200).send('Forward Auth Server is running');
});

// Create server
const server = http.createServer({
    maxHeaderSize: 16 * 1024 // Example: 16 KB (default is 8 KB)
}, app);

// Start the server
server.listen(PORT, () => {
    console.log(`Forward Auth server running on port ${PORT}`);
});
