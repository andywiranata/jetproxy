package proxy.middleware.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleFactory {

    // Creates a list of rules from a rule string and also returns the logical operators between them
    public static RuleContext createRulesFromString(String ruleString) {
        List<Rule> rules = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        // Regex to match rule patterns (e.g., Header('Content-Type', 'application/json'))
        Pattern pattern = Pattern.compile("(Header|HeaderPrefix|HeaderRegex|Query|QueryPrefix|QueryRegex|Path|PathPrefix|PathRegex)\\((.*?)\\)");
        Matcher matcher = pattern.matcher(ruleString);

        // Iterate over each match and create the appropriate Rule instance
        while (matcher.find()) {
            String ruleType = matcher.group(1);    // e.g., "Header", "QueryRegex"
            String[] params = matcher.group(2).split(",\\s*");  // e.g., ["Content-Type", "application/json"]

            RuleType type = getRuleType(ruleType);
            String target = params[0].replace("'", "");
            String value = params[1].replace("'", "");

            // Create the rule and add it to the list
            rules.add(new Rule(type, target, value));
        }

        // Find logical operators (&&, ||)
        Matcher operatorMatcher = Pattern.compile("(\\&\\&|\\|\\|)").matcher(ruleString);
        while (operatorMatcher.find()) {
            operators.add(operatorMatcher.group(1));  // e.g., "&&" or "||"
        }

        return new RuleContext(rules, operators);
    }

    // Converts a string rule type into a RuleType enum
    private static RuleType getRuleType(String ruleType) {
        switch (ruleType) {
            case "Header":
                return RuleType.HEADER;
            case "HeaderPrefix":
                return RuleType.HEADER_PREFIX;
            case "HeaderRegex":
                return RuleType.HEADER_REGEX;
            case "Query":
                return RuleType.QUERY;
            case "QueryPrefix":
                return RuleType.QUERY_PREFIX;
            case "QueryRegex":
                return RuleType.QUERY_REGEX;
            case "Path":
                return RuleType.PATH;
            case "PathPrefix":
                return RuleType.PATH_PREFIX;
            case "PathRegex":
                return RuleType.PATH_REGEX;
            default:
                throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        }
    }
}
