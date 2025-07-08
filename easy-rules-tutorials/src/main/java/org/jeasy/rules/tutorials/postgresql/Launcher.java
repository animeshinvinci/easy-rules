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

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.postgresql.PolicyCatalogOrchestratorService;
import org.jeasy.rules.postgresql.RuleExecutionSummary;
import org.jeasy.rules.tutorials.postgresql.InsuranceApplication.Applicant;

import javax.sql.DataSource;

/**
 * PostgreSQL Tutorial Launcher.
 * 
 * This tutorial demonstrates how to use Easy Rules with PostgreSQL as the rules repository.
 * It shows:
 * 1. Setting up a database connection
 * 2. Loading rules from PostgreSQL
 * 3. Executing rules against different scenarios
 * 4. Working with policy catalogs
 * 5. Audit logging and performance monitoring
 *
 * @author Easy Rules Team
 */
public class Launcher {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("Easy Rules PostgreSQL Tutorial");
        System.out.println("=========================================\n");

        DataSource dataSource = null;
        try {
            // 1. Setup database
            dataSource = DatabaseSetup.createDataSource();
            DatabaseSetup.runMigrations(dataSource);
            
            // Load additional tutorial data
            TutorialDataLoader tutorialLoader = new TutorialDataLoader(dataSource);
            tutorialLoader.loadTutorialData();

            // 2. Create orchestrator service
            PolicyCatalogOrchestratorService orchestrator = new PolicyCatalogOrchestratorService(dataSource);

            System.out.println("Available catalogs: " + orchestrator.getAvailableCatalogs());
            System.out.println();

            // 3. Run insurance underwriting scenarios
            runInsuranceUnderwritingDemo(orchestrator);

            // 4. Run fraud detection scenarios
            runFraudDetectionDemo(orchestrator);

            // 5. Show cache operations
            demonstrateCacheOperations(orchestrator);

        } catch (Exception e) {
            System.err.println("Tutorial failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 6. Cleanup
            if (dataSource != null) {
                DatabaseSetup.shutdown(dataSource);
            }
        }
    }

    private static void runInsuranceUnderwritingDemo(PolicyCatalogOrchestratorService orchestrator) {
        System.out.println("=== Insurance Underwriting Demo ===\n");

        // Scenario 1: Young professional - should be approved
        System.out.println("Scenario 1: Young professional with good credit");
        InsuranceApplication app1 = new InsuranceApplication(
            new Applicant("Alice Johnson", 28, "engineer", 750)
        );
        
        Facts facts1 = new Facts();
        facts1.put("applicant", app1.getApplicant());
        facts1.put("application", app1);
        
        RuleExecutionSummary summary1 = orchestrator.executeRules("insurance_underwriting", facts1);
        System.out.println("Result: " + summary1);
        System.out.println("Application: " + app1);
        System.out.println();

        // Scenario 2: High-risk profession - should require manual review
        System.out.println("Scenario 2: High-risk profession (pilot)");
        InsuranceApplication app2 = new InsuranceApplication(
            new Applicant("Bob Smith", 35, "pilot", 700)
        );
        
        Facts facts2 = new Facts();
        facts2.put("applicant", app2.getApplicant());
        facts2.put("application", app2);
        
        RuleExecutionSummary summary2 = orchestrator.executeRules("insurance_underwriting", facts2);
        System.out.println("Result: " + summary2);
        System.out.println("Application: " + app2);
        System.out.println();

        // Scenario 3: Poor credit score - should be rejected
        System.out.println("Scenario 3: Poor credit score");
        InsuranceApplication app3 = new InsuranceApplication(
            new Applicant("Charlie Brown", 30, "teacher", 550)
        );
        
        Facts facts3 = new Facts();
        facts3.put("applicant", app3.getApplicant());
        facts3.put("application", app3);
        
        RuleExecutionSummary summary3 = orchestrator.executeRules("insurance_underwriting", facts3);
        System.out.println("Result: " + summary3);
        System.out.println("Application: " + app3);
        System.out.println();

        // Scenario 4: Underage applicant
        System.out.println("Scenario 4: Underage applicant");
        InsuranceApplication app4 = new InsuranceApplication(
            new Applicant("Diana Prince", 17, "student", 650)
        );
        
        Facts facts4 = new Facts();
        facts4.put("applicant", app4.getApplicant());
        facts4.put("application", app4);
        
        RuleExecutionSummary summary4 = orchestrator.executeRules("insurance_underwriting", facts4);
        System.out.println("Result: " + summary4);
        System.out.println("Application: " + app4);
        System.out.println();
    }

    private static void runFraudDetectionDemo(PolicyCatalogOrchestratorService orchestrator) {
        System.out.println("=== Fraud Detection Demo ===\n");

        // Scenario 1: Normal transaction
        System.out.println("Scenario 1: Normal transaction");
        Facts facts1 = new Facts();
        facts1.put("transaction", new Transaction(1500.0, "USA"));
        facts1.put("account", new Account("ACC123", 2000.0, 3, 0));
        
        RuleExecutionSummary summary1 = orchestrator.executeRules("fraud_detection", facts1);
        System.out.println("Result: " + summary1);
        System.out.println();

        // Scenario 2: Unusual large transaction
        System.out.println("Scenario 2: Unusually large transaction");
        Facts facts2 = new Facts();
        facts2.put("transaction", new Transaction(50000.0, "USA"));
        facts2.put("account", new Account("ACC456", 5000.0, 2, 0));
        
        RuleExecutionSummary summary2 = orchestrator.executeRules("fraud_detection", facts2);
        System.out.println("Result: " + summary2);
        System.out.println();

        // Scenario 3: High velocity transactions
        System.out.println("Scenario 3: High velocity transactions");
        Facts facts3 = new Facts();
        facts3.put("transaction", new Transaction(500.0, "USA"));
        facts3.put("account", new Account("ACC789", 1000.0, 15, 2));
        
        RuleExecutionSummary summary3 = orchestrator.executeRules("fraud_detection", facts3);
        System.out.println("Result: " + summary3);
        System.out.println();

        // Scenario 4: Multiple failed logins (using specific rule)
        System.out.println("Scenario 4: Multiple failed login attempts (specific rule execution)");
        Facts facts4 = new Facts();
        facts4.put("account", new Account("ACC999", 1000.0, 3, 8));
        
        RuleExecutionSummary summary4 = orchestrator.executeRule("fraud_detection", "multiple_failed_logins_rule", facts4);
        System.out.println("Result: " + summary4);
        System.out.println();
    }

    private static void demonstrateCacheOperations(PolicyCatalogOrchestratorService orchestrator) {
        System.out.println("=== Cache Operations Demo ===\n");

        Facts testFacts = new Facts();
        testFacts.put("applicant", new Applicant("Test User", 25, "engineer", 700));
        testFacts.put("application", new InsuranceApplication(new Applicant("Test User", 25, "engineer", 700)));

        // First execution (loads from database)
        System.out.println("First execution (loads from database):");
        long start1 = System.currentTimeMillis();
        RuleExecutionSummary summary1 = orchestrator.executeRules("insurance_underwriting", testFacts);
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("Execution time: " + time1 + "ms");
        System.out.println("Result: " + summary1.getResult());
        System.out.println();

        // Second execution (from cache)
        System.out.println("Second execution (from cache):");
        long start2 = System.currentTimeMillis();
        RuleExecutionSummary summary2 = orchestrator.executeRules("insurance_underwriting", testFacts);
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("Execution time: " + time2 + "ms");
        System.out.println("Result: " + summary2.getResult());
        System.out.println();

        // Clear cache and execute again
        System.out.println("After clearing cache:");
        orchestrator.clearCache();
        long start3 = System.currentTimeMillis();
        RuleExecutionSummary summary3 = orchestrator.executeRules("insurance_underwriting", testFacts);
        long time3 = System.currentTimeMillis() - start3;
        System.out.println("Execution time: " + time3 + "ms");
        System.out.println("Result: " + summary3.getResult());
        System.out.println();

        System.out.println("Cache performance improvement: " + 
            (time1 > time2 ? "Yes (~" + (time1 - time2) + "ms faster)" : "Marginal"));
    }

    // Helper classes for the demo
    public static class Transaction {
        public final double amount;
        public final String location;
        public String status = "PENDING";

        public Transaction(double amount, String location) {
            this.amount = amount;
            this.location = location;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "Transaction{amount=" + amount + ", location='" + location + "', status='" + status + "'}";
        }
    }

    public static class Account {
        public final String accountNumber;
        public final double averageMonthlyTransactions;
        public final int transactionsToday;
        public final int failedLoginAttempts;
        public String status = "ACTIVE";

        public Account(String accountNumber, double averageMonthlyTransactions, int transactionsToday, int failedLoginAttempts) {
            this.accountNumber = accountNumber;
            this.averageMonthlyTransactions = averageMonthlyTransactions;
            this.transactionsToday = transactionsToday;
            this.failedLoginAttempts = failedLoginAttempts;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "Account{accountNumber='" + accountNumber + "', status='" + status + "', failedLogins=" + failedLoginAttempts + "}";
        }
    }
}