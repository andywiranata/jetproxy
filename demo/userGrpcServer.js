const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const { v4: uuidv4 } = require('uuid');
const path = require('path');

var reflection = require('@grpc/reflection');

// Load proto files
const PROTO_PATH = './user_service.proto';
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true,
});
const userServiceProto = grpc.loadPackageDefinition(packageDefinition).userservice;
reflection = new reflection.ReflectionService(packageDefinition);

// In-memory user database
const users = {};

// Implement CRUD operations
const userService = {
    CreateUser: (call, callback) => {
        console.log('Metadata:', call.metadata.getMap()); // Log metadata
        const user = call.request;
        user.id = uuidv4(); // Generate a unique ID
        users[user.id] = user;
        callback(null, { user });
    },

    GetUser: (call, callback) => {
        console.log('Metadata:', call.metadata.getMap()); // Log metadata
        const { id } = call.request;
        if (!users[id]) {
            return callback({ code: grpc.status.NOT_FOUND, message: 'User not found' });
        }
        callback(null, { user: users[id] });
    },

    UpdateUser: (call, callback) => {
        console.log('Metadata:', call.metadata.getMap()); // Log metadata
        const user = call.request;
        if (!users[user.id]) {
            return callback({ code: grpc.status.NOT_FOUND, message: 'User not found' });
        }
        users[user.id] = user;
        callback(null, { user });
    },

    DeleteUser: (call, callback) => {
        console.log('Metadata:', call.metadata.getMap()); // Log metadata
        const { id } = call.request;
        if (!users[id]) {
            return callback({ code: grpc.status.NOT_FOUND, message: 'User not found' });
        }
        delete users[id];
        callback(null, { message: `User with ID ${id} deleted` });
    },

    ListUsers: (call, callback) => {
        console.log('Metadata:', call.metadata.getMap()); // Log metadata
        const userList = Object.values(users);
        callback(null, { users: userList });
    },
};

// Start the gRPC server
const startServer = async () => {
    const server = new grpc.Server();

    // Add UserService to the server
    server.addService(userServiceProto.UserService.service, userService);

    // Add Reflection Service
    reflection.addToServer(server);

    const address = '0.0.0.0:50051';
    server.bindAsync(address, grpc.ServerCredentials.createInsecure(), (err, port) => {
        if (err) {
            console.error('Failed to start server:', err);
            return;
        }
        console.log(`gRPC server with reflection running at ${address}`);
        server.start();
    });
};

startServer();
