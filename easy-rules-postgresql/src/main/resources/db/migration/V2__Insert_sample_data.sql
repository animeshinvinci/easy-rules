-- Sample data for testing and demonstration

INSERT INTO policy_catalog (name, description, version) VALUES 
('insurance_underwriting', 'Insurance underwriting business rules', '1.0'),
('fraud_detection', 'Fraud detection and prevention rules', '1.0'),
('loan_approval', 'Loan approval processing rules', '1.0');

INSERT INTO rule_definitions (catalog_id, name, description, priority, condition_expression) VALUES 
(1, 'age_validation_rule', 'Validate applicant age for insurance', 100, 'applicant.age >= 18 && applicant.age <= 65'),
(1, 'high_risk_profession_rule', 'Check for high-risk professions', 200, 'Arrays.asList("pilot", "miner", "stuntman").contains(applicant.profession)'),
(1, 'credit_score_rule', 'Check minimum credit score requirement', 150, 'applicant.creditScore >= 650'),
(2, 'unusual_transaction_rule', 'Detect unusual transaction patterns', 100, 'transaction.amount > account.averageMonthlyTransactions * 5'),
(2, 'velocity_check_rule', 'Check transaction velocity', 150, 'account.transactionsToday > 10'),
(3, 'income_verification_rule', 'Verify minimum income requirement', 100, 'applicant.monthlyIncome >= loan.monthlyPayment * 3'),
(3, 'debt_to_income_rule', 'Check debt-to-income ratio', 200, '(applicant.monthlyDebt + loan.monthlyPayment) / applicant.monthlyIncome <= 0.43');

INSERT INTO rule_actions (rule_id, action_order, action_expression, description) VALUES 
(1, 1, 'facts.put("ageValidationResult", "PASS")', 'Set age validation result'),
(2, 1, 'facts.put("riskLevel", "HIGH")', 'Mark as high risk'),
(2, 2, 'facts.put("requiresManualReview", true)', 'Flag for manual review'),
(3, 1, 'facts.put("creditScoreValidation", "PASS")', 'Mark credit score as valid'),
(4, 1, 'facts.put("fraudSuspicion", "HIGH")', 'Mark transaction as suspicious'),
(4, 2, 'System.out.println("ALERT: Suspicious transaction detected for account: " + account.accountNumber)', 'Log fraud alert'),
(5, 1, 'facts.put("velocityCheck", "FAILED")', 'Mark velocity check as failed'),
(5, 2, 'facts.put("blockTransaction", true)', 'Block further transactions'),
(6, 1, 'facts.put("incomeVerification", "PASS")', 'Mark income verification as passed'),
(7, 1, 'facts.put("debtToIncomeRatio", "ACCEPTABLE")', 'Mark debt-to-income ratio as acceptable');