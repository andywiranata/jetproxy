import type { Config } from '../types/proxy';

const API_URL = import.meta.env.VITE_API_URL;
const initialConfig: Config = {
  appName: "${JET_APP_NAME:API-PROXY}",
  port: "${JET_PORT:8080}",
  defaultTimeout: "${JET_DEFAULT_TIMEOUT:10000}",
  dashboard: "${JET_DASHBOARD:true}",
  rootPath: "${JET_DASHBOARD:/}",
  debugMode: "${JET_DEBUG_MODE:true}",
  corsFilter: {
    accessControlAllowMethods: ["*"],
    accessControlAllowHeaders: ["*"],
    accessControlAllowOriginList: ["*"]
  },
  storage: {
    redis: {
      enabled: false,
      host: "localhost",
      port: 6379,
      database: 1,
      maxTotal: 128,
      maxIdle: 64,
      minIdle: 16
    },
    statsd: {
      enabled: false,
      host: "localhost",
      port: 8011,
      prefix: "my.prefix"
    },
    inMemory: {
      enabled: true,
      maxMemory: 50,
      size: 10000
    }
  },
  proxies: [
    {
      path: "/user",
      service: "userApi",
      middleware: {
        jwtAuth: {
          enabled: true
        },
        forwardAuth: {
          enabled: false,
          path: "/verify",
          service: "authApi",
          requestHeaders: "Forward(X-Custom-*);Forward(Authorization);",
          responseHeaders: "Remove(X-Powered-By)"
        },
        rateLimiter: {
          enabled: false,
          limitRefreshPeriod: 200000,
          limitForPeriod: 5,
          maxBurstCapacity: 6
        },
        circuitBreaker: {
          enabled: false,
          failureThreshold: 30,
          slowCallThreshold: 50,
          slowCallDuration: 500,
          openStateDuration: 5,
          waitDurationInOpenState: 10000,
          permittedNumberOfCallsInHalfOpenState: 2,
          minimumNumberOfCalls: 4
        },
        header: {
          requestHeaders: "Remove(x-header-*);Remove(Authorization);Append(X-Custom-Header,-jetty)",
          responseHeaders: "Add(X-Powered-By,jetty-server)"
        }
      },
      ttl: -1
    }
  ],
  services: [
    {
      name: "tasksApi",
      url: "http://localhost:5173/template/1on1-meeting-agenda/tasks.json",
      methods: ["GET", "POST", "PUT"],
      healthcheck: "/ping"
    },
    {
      name: "productApi",
      url: "https://dummyjson.com/products",
      methods: ["GET"],
      role: "userA",
      healthcheck: "/ping"
    },
    {
      name: "googleApi",
      url: "http://www.google.com",
      methods: ["GET", "POST", "PUT"]
    },
    {
      name: "exampleApi",
      url: "http://example.com",
      methods: ["GET", "POST", "PUT"]
    },
    {
      name: "httpbinApi",
      url: "http://httpbin.org",
      methods: ["GET", "POST", "PUT"]
    },
    {
      name: "authApi",
      url: "http://localhost:30001",
      methods: ["POST"]
    },
    {
      name: "userApi",
      url: "http://localhost:30001",
      methods: ["GET"]
    }
  ],
  users: [
    {
      username: "admin",
      password: "admin",
      role: "administrator"
    }
  ],
  jwtAuthSource: {
    headerName: "Authorization",
    tokenPrefix: "Bearer ",
    jwksUri: "https://andy-test.auth0.com/.well-known/jwks.json",
    jwksTtl: -1,
    jwksType: "jwk"
  }
};
  
export async function fetchConfig(): Promise<Config> {
  // const response = await fetch(`${API_URL}/config`);
  // if (!response.ok) {
  //   throw new Error('Failed to fetch configuration');
  // }
  // return response.json();
  return initialConfig;
}

export async function updateConfig(config: Config): Promise<Config> {
  const response = await fetch(`${API_URL}/config`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(config),
  });
  if (!response.ok) {
    throw new Error('Failed to update configuration');
  }
  return response.json();
}