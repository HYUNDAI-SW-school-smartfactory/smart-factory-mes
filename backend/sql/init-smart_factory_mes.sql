CREATE DATABASE IF NOT EXISTS smart_factory_mes
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_factory_mes;

CREATE TABLE IF NOT EXISTS production_lines (
    line_id BIGINT NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    line_name VARCHAR(100) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    current_status VARCHAR(30) NOT NULL,
    target_production INT NOT NULL,
    location VARCHAR(100) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_production_lines PRIMARY KEY (line_id),
    CONSTRAINT uk_production_lines_line_code UNIQUE (line_code),
    CONSTRAINT chk_production_lines_status
        CHECK (current_status IN ('RUN', 'STOP', 'IDLE', 'ERROR', 'MAINTENANCE'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipments (
    equipment_id BIGINT NOT NULL,
    line_id BIGINT NOT NULL,
    equipment_code VARCHAR(50) NOT NULL,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(50) NOT NULL,
    current_status VARCHAR(30) NOT NULL,
    process_order INT NOT NULL,
    last_status_changed_at DATETIME NULL,
    last_inspection_at DATETIME NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_equipments PRIMARY KEY (equipment_id),
    CONSTRAINT uk_equipments_equipment_code UNIQUE (equipment_code),
    CONSTRAINT fk_equipments_line_id
        FOREIGN KEY (line_id) REFERENCES production_lines (line_id),
    CONSTRAINT chk_equipments_status
        CHECK (current_status IN ('RUN', 'STOP', 'IDLE', 'ERROR', 'MAINTENANCE')),
    CONSTRAINT chk_equipments_type
        CHECK (equipment_type IN ('COIL', 'PRESS', 'ROBOT', 'CONVEYOR', 'PACKER', 'LABELER', 'PALLETIZER', 'INSPECTOR')),
    INDEX idx_equipments_line_id_process_order (line_id, process_order, equipment_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS production_records (
    record_id BIGINT NOT NULL AUTO_INCREMENT,
    line_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    record_time DATETIME NOT NULL,
    production_count INT NOT NULL DEFAULT 0,
    defect_count INT NOT NULL DEFAULT 0,
    operation_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    uph INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_production_records PRIMARY KEY (record_id),
    CONSTRAINT fk_production_records_line_id
        FOREIGN KEY (line_id) REFERENCES production_lines (line_id),
    CONSTRAINT fk_production_records_equipment_id
        FOREIGN KEY (equipment_id) REFERENCES equipments (equipment_id),
    CONSTRAINT chk_production_records_production_count CHECK (production_count >= 0),
    CONSTRAINT chk_production_records_defect_count CHECK (defect_count >= 0),
    CONSTRAINT chk_production_records_operation_rate CHECK (operation_rate >= 0.00 AND operation_rate <= 100.00),
    CONSTRAINT chk_production_records_uph CHECK (uph >= 0),
    INDEX idx_production_records_record_time (record_time),
    INDEX idx_production_records_line_id_record_time (line_id, record_time),
    INDEX idx_production_records_equipment_id_record_time (equipment_id, record_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipment_status_history (
    status_history_id BIGINT NOT NULL AUTO_INCREMENT,
    equipment_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    duration_seconds INT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_equipment_status_history PRIMARY KEY (status_history_id),
    CONSTRAINT fk_equipment_status_history_equipment_id
        FOREIGN KEY (equipment_id) REFERENCES equipments (equipment_id),
    CONSTRAINT chk_equipment_status_history_status
        CHECK (status IN ('RUN', 'STOP', 'IDLE', 'ERROR', 'MAINTENANCE')),
    CONSTRAINT chk_equipment_status_history_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    INDEX idx_equipment_status_history_equipment_id_started_at (equipment_id, started_at),
    INDEX idx_equipment_status_history_equipment_id_ended_at (equipment_id, ended_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS alarm_histories (
    alarm_id BIGINT NOT NULL AUTO_INCREMENT,
    line_id BIGINT NOT NULL,
    equipment_id BIGINT NULL,
    alarm_type VARCHAR(30) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    message VARCHAR(255) NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_alarm_histories PRIMARY KEY (alarm_id),
    CONSTRAINT fk_alarm_histories_line_id
        FOREIGN KEY (line_id) REFERENCES production_lines (line_id),
    CONSTRAINT fk_alarm_histories_equipment_id
        FOREIGN KEY (equipment_id) REFERENCES equipments (equipment_id),
    CONSTRAINT chk_alarm_histories_type
        CHECK (alarm_type IN ('STOP', 'WARNING', 'ERROR', 'DEFECT', 'MAINTENANCE')),
    CONSTRAINT chk_alarm_histories_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    INDEX idx_alarm_histories_line_id_created_at (line_id, created_at),
    INDEX idx_alarm_histories_equipment_id_created_at (equipment_id, created_at)
) ENGINE=InnoDB;
