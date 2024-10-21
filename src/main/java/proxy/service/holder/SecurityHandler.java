package proxy.service.holder;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;

import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.util.security.Credential;

public class SecurityHandler {

    public  void createBasicAuthSecurityHandler() {
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        // Map the constraint to all paths
        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        // Create and configure the security handler
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticator(new BasicAuthenticator());

        // Use a PropertyFileLoginModule and point to the realm.properties
        LoginService loginService = new HashLoginService("MyRealm",
                "src/main/resources/realm.properties");
        securityHandler.setLoginService(loginService);
        securityHandler.addConstraintMapping(constraintMapping);


//        return securityHandler;
    }

}
