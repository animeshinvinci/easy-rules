-- PostgreSQL Schema for Database-Driven Rules Engine

-- Policy catalog table for rule organization
CREATE TABLE policy_catalog (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    version VARCHAR(50) NOT NULL DEFAULT '1.0',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, DRAFT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Rule definitions table
CREATE TABLE rule_definitions (
    id BIGSERIAL PRIMARY KEY,
    catalog_id BIGINT NOT NULL REFERENCES policy_catalog(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority INTEGER NOT NULL DEFAULT 1000,
    rule_type VARCHAR(50) NOT NULL DEFAULT 'SIMPLE', -- SIMPLE, COMPOSITE
    composite_rule_type VARCHAR(50), -- UnitRuleGroup, ActivationRuleGroup, ConditionalRuleGroup
    condition_expression TEXT, -- MVEL expression for rule condition
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(catalog_id, name)
);

-- Rule actions table (one-to-many with rule_definitions)
CREATE TABLE rule_actions (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES rule_definitions(id) ON DELETE CASCADE,
    action_order INTEGER NOT NULL DEFAULT 1,
    action_expression TEXT NOT NULL, -- MVEL expression for action
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Composite rule relationships (for composite rules)
CREATE TABLE rule_compositions (
    id BIGSERIAL PRIMARY KEY,
    parent_rule_id BIGINT NOT NULL REFERENCES rule_definitions(id) ON DELETE CASCADE,
    child_rule_id BIGINT NOT NULL REFERENCES rule_definitions(id) ON DELETE CASCADE,
    rule_order INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(parent_rule_id, child_rule_id)
);

-- Rule execution audit log
CREATE TABLE rule_execution_log (
    id BIGSERIAL PRIMARY KEY,
    catalog_id BIGINT REFERENCES policy_catalog(id),
    rule_id BIGINT REFERENCES rule_definitions(id),
    execution_context JSONB, -- Facts used during execution
    execution_result VARCHAR(20), -- SUCCESS, FAILED, SKIPPED
    execution_time_ms BIGINT,
    error_message TEXT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_policy_catalog_name ON policy_catalog(name);
CREATE INDEX idx_policy_catalog_status ON policy_catalog(status);
CREATE INDEX idx_rule_definitions_catalog_priority ON rule_definitions(catalog_id, priority);
CREATE INDEX idx_rule_definitions_status ON rule_definitions(status);
CREATE INDEX idx_rule_actions_rule_order ON rule_actions(rule_id, action_order);
CREATE INDEX idx_rule_execution_log_catalog_time ON rule_execution_log(catalog_id, executed_at);