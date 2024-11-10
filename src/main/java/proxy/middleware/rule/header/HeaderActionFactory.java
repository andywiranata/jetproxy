package proxy.middleware.rule.header;


import proxy.middleware.rule.RuleContext;
import proxy.middleware.rule.RuleFactory;

import java.util.ArrayList;
import java.util.List;

// https://chatgpt.com/c/672c6e21-7b54-800b-b1ab-4f625b29819e
public class HeaderActionFactory {
    public static List<HeaderAction> createActions(String config) {
        List<HeaderAction> actions = new ArrayList<>();

        String[] rules = config.split(";");
        for (String rule : rules) {
            if (rule.startsWith("Forward(")) {
                String[] parts = rule.substring(8, rule.length() - 1).split(",", 2);
                String pattern = parts[0];
                RuleContext ruleContext = parts.length > 1 ? RuleFactory.createRulesFromString(parts[1]) : null;
                actions.add(new ForwardHeader(pattern, ruleContext));
            } else if (rule.startsWith("Copy(")) {
                String[] parts = rule.substring(5, rule.length() - 1).split(",", 3);
                String sourcePattern = parts[0];
                String targetPrefix = parts[1];
                RuleContext ruleContext = parts.length > 2 ? RuleFactory.createRulesFromString(parts[2]) : null;
                actions.add(new CopyHeader(sourcePattern, targetPrefix, ruleContext));
            } else if (rule.startsWith("Append(")) {
                String[] parts = rule.substring(7, rule.length() - 1).split(",", 3);
                String pattern = parts[0];
                String valueToAppend = parts[1];
                RuleContext ruleContext = parts.length > 2 ? RuleFactory.createRulesFromString(parts[2]) : null;
                actions.add(new AppendHeader(pattern, valueToAppend, ruleContext));
            } else if (rule.startsWith("Modify(")) {
                String[] parts = rule.substring(7, rule.length() - 1).split(",", 3);
                String pattern = parts[0];
                String replacement = parts[1];
                String newValue = parts[2];
                RuleContext ruleContext = parts.length > 3 ? RuleFactory.createRulesFromString(parts[3]) : null;
                actions.add(new ModifyHeader(pattern, value -> value.replace(replacement, newValue), ruleContext));
            }
        }

        return actions;
    }
}
