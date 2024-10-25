---
sidebar_position: 1
---

# Overview

JetProxy offers flexible and powerful routing capabilities, allowing you to define custom routes for your HTTP requests based on paths, headers, or other request attributes. This ensures that traffic is directed to the correct backend service without the complexity of load balancing.

Compared to conventional reverse proxies like Nginx or HAProxy, JetProxy provides a much simpler and developer-friendly experience:

Ease of Configuration: With JetProxy, routing configurations are straightforward and written in a user-friendly YAML format, making it easy to define routes, apply caching, and set up complex rules without needing to learn complex syntax or configuration files like those required by Nginx or HAProxy.

* Dynamic Routing: JetProxy allows dynamic and flexible routing based on headers, query parameters, and custom conditions without the need for complex scripts or external modules. This is a key advantage over traditional reverse proxies that often require additional scripting for advanced routing scenarios.

* Rapid Setup: With JetProxy, developers can quickly configure routes and deploy the proxy with minimal setup time. In contrast, conventional proxies often involve multiple configuration steps and tuning for each new service or rule.

Example Comparison:

* In Nginx, you would need to define a complex configuration file and possibly use custom scripting for advanced routing. In JetProxy, a simple YAML file with clearly defined services and routes gets the job done, making it accessible even for developers who are not experts in infrastructure management.

JetProxy simplifies the proxying process for modern web applications, enabling faster setup and configuration with greater flexibility.