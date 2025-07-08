# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Easy Rules is a Java rules engine that provides a simple API for creating and executing business rules. It's a multi-module Maven project in maintenance mode (only bug fixes accepted).

## Build and Development Commands

### Build
- `mvn clean compile` - Compile all modules
- `mvn clean package` - Build all modules with tests
- `mvn clean install` - Install all modules to local repository

### Testing
- `mvn test` - Run all tests in all modules
- `mvn test -pl easy-rules-core` - Run tests for core module only
- `mvn surefire:test` - Run unit tests with Surefire plugin

### Individual Module Development
- `cd easy-rules-core && mvn test` - Work with core module
- `cd easy-rules-mvel && mvn test` - Work with MVEL module
- `cd easy-rules-spel && mvn test` - Work with SpEL module
- `cd easy-rules-jexl && mvn test` - Work with JEXL module

### Code Quality
- License headers are automatically formatted during build via license-maven-plugin
- Code compiles with Java 1.8 compatibility
- Tests use JUnit 4.13.1, AssertJ 3.18.1, and Mockito 3.6.0

## Architecture

### Core Components

**easy-rules-core** - The foundation module containing:
- `Rule` interface - Defines `evaluate(Facts)` and `execute(Facts)` methods
- `Facts` - Container for named facts (key-value pairs) used in rule evaluation
- `RulesEngine` interface - Defines `fire(Rules, Facts)` and `check(Rules, Facts)`
- `DefaultRulesEngine` - Main implementation that evaluates rules by priority and executes actions
- `RuleBuilder` - Fluent API for programmatic rule creation
- Annotation-based rule definition via `@Rule`, `@Condition`, `@Action`, `@Priority`

**Rules Execution Flow:**
1. Rules are sorted by priority (lower numbers = higher priority)
2. Each rule's condition is evaluated against provided facts
3. If condition is true, rule's actions are executed
4. Process continues based on engine parameters (skip on first applied/failed, etc.)

### Extension Modules

**easy-rules-mvel** - MVEL expression language support
- `MVELRule` - Rules defined with MVEL expressions
- `MVELRuleFactory` - Creates rules from JSON/YAML descriptors

**easy-rules-spel** - Spring Expression Language support
- `SpELRule` - Rules using SpEL expressions
- Integration with Spring's expression evaluation

**easy-rules-jexl** - Apache JEXL expression language support
- `JexlRule` - Rules using JEXL expressions
- Latest addition to expression language support

**easy-rules-support** - Common utilities
- Composite rule patterns (ActivationRuleGroup, ConditionalRuleGroup, UnitRuleGroup)
- Rule definition readers for JSON/YAML formats
- Abstract factories for rule creation

### Key Design Patterns

- **Strategy Pattern**: Different rule evaluation strategies via RulesEngine implementations
- **Builder Pattern**: RuleBuilder for fluent rule construction
- **Template Method**: AbstractRulesEngine defines execution template
- **Composite Pattern**: Composite rules that group multiple rules with logic operators

## Development Guidelines

### Testing Strategy
- Each module has comprehensive unit tests in src/test/java
- Test classes follow naming convention: `*Test.java`
- Uses JUnit 4 with AssertJ assertions and Mockito mocking
- Test resources include YAML/JSON rule definition examples

### Module Dependencies
- Core module has no external dependencies except SLF4J for logging
- Extension modules depend on their respective expression language libraries
- Support module provides shared utilities for rule factories and readers

### Rule Definition Approaches
1. **Annotation-based**: Use `@Rule`, `@Condition`, `@Action` on POJOs
2. **Programmatic**: Use `RuleBuilder` with fluent API
3. **Expression-based**: Use MVEL/SpEL/JEXL with rule factories
4. **Descriptor-based**: Define rules in YAML/JSON files

### Common Development Patterns
When adding new functionality, follow existing patterns:
- Extend appropriate abstract classes (AbstractRulesEngine, AbstractRuleFactory)
- Use builder pattern for complex object construction
- Implement proper null checks and validation
- Add comprehensive unit tests with good coverage
- Follow existing logging patterns using SLF4J