# Easy Rules PostgreSQL Module

This module extends Easy Rules with PostgreSQL database-driven rule repository capabilities. It allows you to store, manage, and execute business rules using PostgreSQL as the data source.

## Features

- **Database-driven rules**: Store rules in PostgreSQL with full CRUD operations
- **Policy catalog organization**: Group rules into logical catalogs for better management
- **MVEL expression support**: Leverage MVEL for dynamic rule conditions and actions
- **Composite rules**: Support for complex rule hierarchies and compositions
- **Execution auditing**: Track rule executions with detailed logs and metrics
- **Caching support**: Optional caching for improved performance
- **Integration testing**: Comprehensive test suite using Testcontainers

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.jeasy</groupId>
    <artifactId>easy-rules-postgresql</artifactId>
    <version>4.1.1-SNAPSHOT</version>
</dependency>
```

### 2. Database Setup

Run the included Flyway migrations to set up the database schema:

```java
Flyway flyway = Flyway.configure()
    .dataSource(dataSource)
    .locations("classpath:db/migration")
    .load();
flyway.migrate();
```

### 3. Basic Usage

```java
// Create data source
DataSource dataSource = // your PostgreSQL DataSource

// Create orchestrator service
PolicyCatalogOrchestratorService orchestrator = 
    new PolicyCatalogOrchestratorService(dataSource);

// Prepare facts
Facts facts = new Facts();
facts.put("applicant", new Applicant(25, "engineer", 750));

// Execute rules from a policy catalog
RuleExecutionSummary summary = orchestrator.executeRules("insurance_underwriting", facts);

// Check results
if (summary.isSuccessful()) {
    System.out.println("Executed " + summary.getTotalRulesCount() + " rules");
    System.out.println("Triggered " + summary.getTriggeredRulesCount() + " rules");
}
```

## Database Schema

The module uses the following main tables:

- **policy_catalog**: Organizes rules into logical groups
- **rule_definitions**: Stores rule metadata and conditions
- **rule_actions**: Stores rule actions (MVEL expressions)
- **rule_compositions**: Manages composite rule relationships
- **rule_execution_log**: Audit trail for rule executions

## Advanced Usage

### Rule Factory

For direct rule creation without orchestration:

```java
PostgreSQLRuleFactory factory = new PostgreSQLRuleFactory(dataSource);

// Create specific rule
Rule rule = factory.createRule("insurance_underwriting", "age_validation_rule");

// Create all rules from catalog
Rules rules = factory.createRules("fraud_detection");
```

### Custom MVEL Context

```java
ParserContext parserContext = new ParserContext();
// Configure MVEL imports, etc.

PostgreSQLRuleFactory factory = new PostgreSQLRuleFactory(dataSource, parserContext);
```

### Caching Control

```java
// Disable caching
PolicyCatalogOrchestratorService orchestrator = 
    new PolicyCatalogOrchestratorService(dataSource, false);

// Clear cache
orchestrator.clearCache();

// Refresh specific catalog
orchestrator.refreshCatalogCache("insurance_underwriting");
```

## Sample Data

The module includes sample data for testing:

- **Insurance Underwriting Rules**: Age validation, risk assessment, credit checks
- **Fraud Detection Rules**: Transaction monitoring, velocity checks
- **Loan Approval Rules**: Income verification, debt-to-income ratios

## Testing

Run tests with:

```bash
mvn test
```

Tests use Testcontainers to spin up real PostgreSQL instances, ensuring integration reliability.

## Performance Considerations

- Enable caching for frequently accessed catalogs
- Use connection pooling (HikariCP recommended)
- Index strategy is optimized for rule lookup patterns
- Consider partitioning audit logs for high-volume scenarios

## Migration from File-based Rules

To migrate existing JSON/YAML rules to PostgreSQL:

1. Create policy catalog entries
2. Insert rule definitions with conditions and actions
3. Update application code to use `PostgreSQLRuleFactory`
4. Test rule execution behavior matches original files