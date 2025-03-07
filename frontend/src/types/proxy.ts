export interface CorsFilter {
  accessControlAllowMethods: string[];
  accessControlAllowHeaders: string[];
  accessControlAllowOriginList: string[];
}

export interface RedisStorage {
  enabled: boolean;
  host: string;
  port: number;
  database: number;
  maxTotal: number;
  maxIdle: number;
  minIdle: number;
}

export interface StatsdStorage {
  enabled: boolean;
  host: string;
  port: number;
  prefix: string;
}

export interface InMemoryStorage {
  enabled: boolean;
  maxMemory: number;
  size: number;
}

export interface Storage {
  redis: RedisStorage;
  statsd: StatsdStorage;
  inMemory: InMemoryStorage;
}

export interface Middleware {
  jwtAuth?: {
    enabled: boolean;
  };
  forwardAuth?: {
    enabled?: boolean;
    path: string;
    service: string;
    requestHeaders?: string;
    responseHeaders?: string;
  };
  rateLimiter?: {
    enabled: boolean;
    limitRefreshPeriod: number;
    limitForPeriod: number;
    maxBurstCapacity: number;
  };
  circuitBreaker?: {
    enabled: boolean;
    failureThreshold: number;
    slowCallThreshold: number;
    slowCallDuration: number;
    openStateDuration: number;
    waitDurationInOpenState: number;
    permittedNumberOfCallsInHalfOpenState: number;
    minimumNumberOfCalls: number;
  };
  header?: {
    requestHeaders: string;
    responseHeaders: string;
  };
  basicAuth?: string;
  rule?: string;
}

export interface Rule {
  match: {
    rule: string;
    service: string;
    path: string;
  };
}

export interface Proxy {
  path: string;
  service: string;
  middleware: Middleware;
  ttl: number;
  rules?: Rule[];
}

export interface Service {
  name: string;
  url: string;
  methods: string[];
  role?: string;
  healthcheck?: string;
}

export interface User {
  username: string;
  password: string;
  role: string;
}

export interface JwtAuthSource {
  headerName: string;
  tokenPrefix: string;
  jwksUri: string;
  jwksTtl: number;
  jwksType: string;
}

export interface Config {
  appName: string;
  port: string;
  defaultTimeout: string;
  dashboard: string;
  rootPath: string;
  debugMode: string;
  corsFilter: CorsFilter;
  storage: Storage;
  proxies: Proxy[];
  services: Service[];
  users: User[];
  jwtAuthSource: JwtAuthSource;
}
