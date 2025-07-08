# PostgreSQL Tutorial

This tutorial demonstrates how to use Easy Rules with PostgreSQL as a database-driven rules repository.

## What You'll Learn

- Setting up database-driven rules with PostgreSQL
- Organizing rules into policy catalogs
- Executing rules with audit logging
- Working with the PolicyCatalogOrchestratorService
- Performance optimization with caching
- Real-world scenarios: Insurance underwriting and fraud detection

## Prerequisites

- Java 8+
- Maven 3.6+
- Basic understanding of Easy Rules concepts

## Running the Tutorial

### Option 1: Using Maven profile

```bash
cd easy-rules-tutorials
mvn clean compile -P runPostgreSQLTutorial
```

### Option 2: Direct execution

```bash
cd easy-rules-tutorials
mvn clean compile exec:java -Dexec.mainClass="org.jeasy.rules.tutorials.postgresql.Launcher"
```

## Tutorial Structure

### 1. Database Setup
- Uses H2 in PostgreSQL compatibility mode for simplicity
- Automatically runs Flyway migrations
- Loads sample data for insurance and fraud detection

### 2. Insurance Underwriting Demo
Shows how rules evaluate different insurance application scenarios:
- **Young Professional**: Good credit, approved automatically
- **High-Risk Profession**: Pilot, requires manual review
- **Poor Credit**: Rejected due to low credit score
- **Underage Applicant**: Age validation failure

### 3. Fraud Detection Demo
Demonstrates fraud detection rules:
- **Normal Transaction**: Passes all checks
- **Large Transaction**: Triggers suspicious amount alerts
- **High Velocity**: Too many transactions trigger blocking
- **Failed Logins**: Account lockout after multiple failures

### 4. Performance & Caching
Shows the impact of caching on rule execution performance.

## Key Components

### PolicyCatalogOrchestratorService
High-level service that manages rule execution:
- Loads rules from database
- Executes rules against facts
- Provides audit logging
- Supports caching for performance

### Rule Organization
Rules are organized into policy catalogs:
- `insurance_underwriting`: Insurance business rules
- `fraud_detection`: Fraud prevention rules
- `loan_approval`: Loan processing rules

### Database Schema
- `policy_catalog`: Rule organization
- `rule_definitions`: Rule metadata and conditions
- `rule_actions`: MVEL action expressions
- `rule_execution_log`: Audit trail

## Sample Output

```
=========================================
Easy Rules PostgreSQL Tutorial
=========================================

Setting up database schema and sample data...
Database setup completed successfully!
Loading tutorial-specific data...
Tutorial data loaded successfully!

=== Insurance Underwriting Demo ===

Scenario 1: Young professional with good credit
Result: RuleExecutionSummary{catalogName='insurance_underwriting', totalRules=6, triggeredRules=3, result='SUCCESS', executionTimeMs=45}
Application: InsuranceApplication{applicant=Applicant{name='Alice Johnson', age=28, profession='engineer', creditScore=750}, status='APPROVED', riskLevel='LOW', requiresManualReview=false, rejectionReason='null'}

[... more scenarios ...]

=== Cache Operations Demo ===

First execution (loads from database):
Execution time: 23ms
Result: SUCCESS

Second execution (from cache):
Execution time: 2ms
Result: SUCCESS

Cache performance improvement: Yes (~21ms faster)
```

## Production Considerations

### Database Connection
In production, replace the H2 setup with actual PostgreSQL:

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:postgresql://localhost:5432/easyrules");
config.setUsername("your_username");
config.setPassword("your_password");
```

### Performance Tuning
- Enable connection pooling
- Use caching for frequently accessed catalogs
- Consider read replicas for high-volume scenarios
- Partition audit logs by date

### Security
- Use proper database credentials
- Implement role-based access control
- Encrypt sensitive rule data
- Audit rule modifications

## Next Steps

1. Try modifying the rules in the database
2. Add your own policy catalogs
3. Experiment with composite rules
4. Integrate with your existing application
5. Set up monitoring and alerting

For more information, see the main Easy Rules PostgreSQL module documentation.