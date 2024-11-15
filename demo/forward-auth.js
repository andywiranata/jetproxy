const express = require('express');
const app = express();
const PORT = process.env.PORT || 30001;

app.use(express.json());

// Route to simulate forward authentication
app.post('/verify', (req, res) => {
    const authHeader = req.headers['authorization'];
    console.log('auth::header::', req.headers )
    console.log('auth::header::',authHeader )

    // Check if the Authorization header is provided
    if (!authHeader) {
        return res.status(401).json({ error: 'Authorization header missing' });
    }
    // Token is present; respond with success
    res.status(200).json({
        message: 'Authentication successful',
        user: {
            id: 'user123',
            name: 'Mock User',
            roles: ['roleA', 'roleB'],
        }
    });
});
app.get('/user', (req, res) =>{
    console.log('user::header::', req.headers )
    res.setHeader('X-Custom-Header', 'CustomValue');
    res.setHeader('X-Powered-By', 'Express with Love');
    res.status(200).json({
        user: {
            id: 'user123',
            name: 'Mock User',
            roles: ['roleA', 'roleB'],
        }
    });
})

// Health check endpoint
app.get('/health', (req, res) => {
    res.status(200).send('Forward Auth Server is running');
});

app.listen(PORT, () => {
    console.log(`Forward Auth server running on port ${PORT}`);
});
