/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.jeasy.rules.tutorials.postgresql;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Loads additional tutorial-specific data into the database.
 * Extends the base sample data with more comprehensive examples.
 *
 * @author Easy Rules Team
 */
public class TutorialDataLoader {

    private final JdbcTemplate jdbcTemplate;

    public TutorialDataLoader(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Load tutorial-specific rules and data.
     */
    public void loadTutorialData() {
        System.out.println("Loading tutorial-specific data...");

        // Add tutorial-specific insurance rules
        addTutorialInsuranceRules();
        
        // Add enhanced fraud detection rules
        addTutorialFraudRules();

        System.out.println("Tutorial data loaded successfully!");
    }

    private void addTutorialInsuranceRules() {
        // Get insurance underwriting catalog ID
        Long catalogId = jdbcTemplate.queryForObject(
            "SELECT id FROM policy_catalog WHERE name = 'insurance_underwriting'", 
            Long.class
        );

        // Add tutorial-specific rules
        String insertRule = "INSERT INTO rule_definitions (catalog_id, name, description, priority, condition_expression) " +
            "VALUES (?, ?, ?, ?, ?)";

        // Low credit score rule
        jdbcTemplate.update(insertRule, catalogId, "low_credit_score_rule", 
            "Reject applications with very low credit scores", 50, 
            "applicant.creditScore < 600");

        Long lowCreditRuleId = jdbcTemplate.queryForObject(
            "SELECT id FROM rule_definitions WHERE name = 'low_credit_score_rule'", 
            Long.class
        );

        // Add actions for low credit score rule
        String insertAction = "INSERT INTO rule_actions (rule_id, action_order, action_expression, description) " +
            "VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(insertAction, lowCreditRuleId, 1, 
            "application.setStatus(\"REJECTED\")", "Reject application");
        jdbcTemplate.update(insertAction, lowCreditRuleId, 2, 
            "application.setRejectionReason(\"Credit score too low\")", "Set rejection reason");

        // Add young driver rule
        jdbcTemplate.update(insertRule, catalogId, "young_driver_rule", 
            "Higher premium for young drivers", 75, 
            "applicant.age < 25 && applicant.profession.equals(\"driver\")");

        Long youngDriverRuleId = jdbcTemplate.queryForObject(
            "SELECT id FROM rule_definitions WHERE name = 'young_driver_rule'", 
            Long.class
        );

        jdbcTemplate.update(insertAction, youngDriverRuleId, 1, 
            "application.setRiskLevel(\"HIGH\")", "Mark as high risk");
        jdbcTemplate.update(insertAction, youngDriverRuleId, 2, 
            "System.out.println(\"Young driver detected: \" + applicant.name)", "Log young driver");

        // Add good customer rule
        jdbcTemplate.update(insertRule, catalogId, "good_customer_rule", 
            "Approve good customers automatically", 300, 
            "applicant.age >= 25 && applicant.age <= 60 && applicant.creditScore >= 700 && !Arrays.asList(\"pilot\", \"miner\", \"stuntman\").contains(applicant.profession)");

        Long goodCustomerRuleId = jdbcTemplate.queryForObject(
            "SELECT id FROM rule_definitions WHERE name = 'good_customer_rule'", 
            Long.class
        );

        jdbcTemplate.update(insertAction, goodCustomerRuleId, 1, 
            "application.setStatus(\"APPROVED\")", "Auto-approve application");
        jdbcTemplate.update(insertAction, goodCustomerRuleId, 2, 
            "application.setRiskLevel(\"LOW\")", "Mark as low risk");
    }

    private void addTutorialFraudRules() {
        // Get fraud detection catalog ID
        Long catalogId = jdbcTemplate.queryForObject(
            "SELECT id FROM policy_catalog WHERE name = 'fraud_detection'", 
            Long.class
        );

        String insertRule = "INSERT INTO rule_definitions (catalog_id, name, description, priority, condition_expression) " +
            "VALUES (?, ?, ?, ?, ?)";

        // Multiple failed login attempts
        jdbcTemplate.update(insertRule, catalogId, "multiple_failed_logins_rule", 
            "Detect multiple failed login attempts", 50, 
            "account.failedLoginAttempts > 5");

        Long failedLoginRuleId = jdbcTemplate.queryForObject(
            "SELECT id FROM rule_definitions WHERE name = 'multiple_failed_logins_rule'", 
            Long.class
        );

        String insertAction = "INSERT INTO rule_actions (rule_id, action_order, action_expression, description) " +
            "VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(insertAction, failedLoginRuleId, 1, 
            "account.setStatus(\"LOCKED\")", "Lock account");
        jdbcTemplate.update(insertAction, failedLoginRuleId, 2, 
            "System.out.println(\"Account locked due to multiple failed login attempts: \" + account.accountNumber)", "Log security event");

        // Suspicious location rule
        jdbcTemplate.update(insertRule, catalogId, "suspicious_location_rule", 
            "Detect transactions from suspicious locations", 75, 
            "Arrays.asList(\"Suspicious Country\", \"Blacklisted Region\").contains(transaction.location)");

        Long suspiciousLocationRuleId = jdbcTemplate.queryForObject(
            "SELECT id FROM rule_definitions WHERE name = 'suspicious_location_rule'", 
            Long.class
        );

        jdbcTemplate.update(insertAction, suspiciousLocationRuleId, 1, 
            "transaction.setStatus(\"BLOCKED\")", "Block transaction");
        jdbcTemplate.update(insertAction, suspiciousLocationRuleId, 2, 
            "System.out.println(\"Transaction blocked from suspicious location: \" + transaction.location)", "Log location fraud");
    }
}