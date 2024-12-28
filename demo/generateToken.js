const jwt = require("jsonwebtoken");

// Configuration for JWT
const config = {
    secretKey: "U2VjdXJlU3Ryb25nS2V5Rm9yVXNpbmdXaXRoSFMyNTY=", // Replace with your actual secure secret key
    algorithms: ["HS256"],          // Algorithm used for signing
    claimValidations: {
        iss: "auth.myapp.com",         // Issuer of the token
    },
};

// Generate a JWT
function generateToken() {
    const payload = {
        sub: "123456",                 // Subject (User ID)
        name: "John Doe",              // Example name claim
        iss: config.claimValidations.iss, // Issuer
        iat: Math.floor(Date.now() / 1000), // Issued at
        exp: Math.floor(Date.now() / 1000) + 3600, // Expires in 1 hour
    };

    const token = jwt.sign(payload, config.secretKey, { algorithm: config.algorithms[0] });
    return token;
}

// Generate and print the token
const token = generateToken();
console.log("Generated JWT:", token);
