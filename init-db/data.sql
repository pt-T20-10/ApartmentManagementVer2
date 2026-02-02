-- =========================
-- APARTMENT MANAGEMENT SYSTEM - DOCKER INIT SCRIPT
-- Database: DB_QuanLyChungCu
-- Version: 2.0
-- Last Updated: 2026-02-02
-- =========================

-- Create database
CREATE DATABASE IF NOT EXISTS DB_QuanLyChungCu
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE DB_QuanLyChungCu;

SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET time_zone = '+00:00';

-- =========================
-- DROP EXISTING OBJECTS (for clean re-run)
-- =========================

DROP VIEW IF EXISTS v_contract_summary;
DROP TRIGGER IF EXISTS trg_contract_after_insert;

-- =========================
-- TABLE CREATION
-- =========================

-- Table: users
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  role VARCHAR(20) DEFAULT 'STAFF',
  assigned_by BIGINT DEFAULT NULL COMMENT 'ID của user đã tạo/gán tài khoản này',
  is_active TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP NULL DEFAULT NULL,
  is_deleted TINYINT(1) DEFAULT 0,
  
  INDEX idx_users_role_building (role),
  INDEX idx_users_assigned_by (assigned_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: buildings
CREATE TABLE IF NOT EXISTS buildings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  address VARCHAR(255),
  manager_name VARCHAR(100),
  description TEXT,
  is_deleted TINYINT(1) DEFAULT 0,
  status VARCHAR(50) DEFAULT 'Đang hoạt động',
  manager_user_id BIGINT,
  
  INDEX idx_manager_user_id (manager_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: floors
CREATE TABLE IF NOT EXISTS floors (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  building_id BIGINT NOT NULL,
  floor_number INT NOT NULL,
  name VARCHAR(50),
  is_deleted TINYINT(1) DEFAULT 0,
  status VARCHAR(50) DEFAULT 'Đang hoạt động',
  
  INDEX idx_building_id (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: apartments
CREATE TABLE IF NOT EXISTS apartments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  floor_id BIGINT NOT NULL,
  room_number VARCHAR(20) NOT NULL,
  area DOUBLE,
  status VARCHAR(20) DEFAULT 'AVAILABLE',
  description TEXT,
  is_deleted TINYINT(1) DEFAULT 0,
  apartment_type VARCHAR(50) DEFAULT 'Standard',
  bedroom_count INT DEFAULT 1,
  bathroom_count INT DEFAULT 1,
  
  INDEX idx_floor_id (floor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: residents
CREATE TABLE IF NOT EXISTS residents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(100),
  identity_card VARCHAR(20) NOT NULL,
  gender VARCHAR(10),
  dob DATE,
  hometown VARCHAR(255),
  is_deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: contracts
CREATE TABLE IF NOT EXISTS contracts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_number VARCHAR(50) UNIQUE,
  apartment_id BIGINT NOT NULL,
  resident_id BIGINT NOT NULL,
  contract_type VARCHAR(20) DEFAULT 'RENTAL',
  signed_date DATE DEFAULT NULL,
  start_date DATE DEFAULT NULL,
  end_date DATE DEFAULT NULL,
  terminated_date DATE DEFAULT NULL,
  notes TEXT,
  deposit_amount DECIMAL(15,2) DEFAULT NULL,
  monthly_rent DECIMAL(15,2) DEFAULT 0.00,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  is_deleted TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_apartment_id (apartment_id),
  INDEX idx_resident_id (resident_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: contract_history
CREATE TABLE IF NOT EXISTS contract_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  old_value TEXT,
  new_value TEXT,
  old_end_date DATE DEFAULT NULL,
  new_end_date DATE DEFAULT NULL,
  reason TEXT,
  created_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_contract_id (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: services
CREATE TABLE IF NOT EXISTS services (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service_name VARCHAR(100) NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  unit_type VARCHAR(20),
  is_mandatory TINYINT(1) DEFAULT 0,
  is_deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: contract_services
CREATE TABLE IF NOT EXISTS contract_services (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  applied_date DATE NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  is_active TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  UNIQUE KEY uk_contract_service (contract_id, service_id, is_active),
  INDEX idx_contract_id (contract_id),
  INDEX idx_service_id (service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: invoices
CREATE TABLE IF NOT EXISTS invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  month INT NOT NULL,
  year INT NOT NULL,
  total_amount DECIMAL(15,2) DEFAULT 0,
  status VARCHAR(20) DEFAULT 'UNPAID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  payment_date TIMESTAMP NULL,
  is_deleted TINYINT(1) DEFAULT 0,
  
  INDEX idx_contract_id (contract_id),
  INDEX idx_month_year (month, year),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: invoice_details
CREATE TABLE IF NOT EXISTS invoice_details (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  service_name VARCHAR(100),
  unit_price DECIMAL(15,2),
  quantity DOUBLE,
  amount DECIMAL(15,2),
  
  INDEX idx_invoice_id (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: service_usage
CREATE TABLE IF NOT EXISTS service_usage (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  month INT NOT NULL,
  year INT NOT NULL,
  old_index DOUBLE DEFAULT 0,
  new_index DOUBLE DEFAULT 0,
  actual_usage DOUBLE DEFAULT NULL,
  
  INDEX idx_contract_id (contract_id),
  INDEX idx_service_id (service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: household_members
CREATE TABLE IF NOT EXISTS household_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  relationship VARCHAR(50) NOT NULL,
  is_head TINYINT(1) DEFAULT 0,
  gender VARCHAR(10),
  dob DATE,
  identity_card VARCHAR(20),
  phone VARCHAR(20),
  is_active TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_contract_id (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: user_buildings
CREATE TABLE IF NOT EXISTS user_buildings (
  user_id BIGINT NOT NULL,
  building_id BIGINT NOT NULL,
  assigned_by BIGINT DEFAULT NULL COMMENT 'User đã gán',
  assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  PRIMARY KEY (user_id, building_id),
  INDEX idx_building_id (building_id),
  INDEX idx_assigned_by (assigned_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================
-- FOREIGN KEY CONSTRAINTS
-- =========================

-- Users self-reference
ALTER TABLE users
  ADD CONSTRAINT fk_users_assigned_by 
  FOREIGN KEY (assigned_by) REFERENCES users(id) 
  ON DELETE SET NULL ON UPDATE CASCADE;

-- Buildings
ALTER TABLE buildings
  ADD CONSTRAINT fk_buildings_manager 
  FOREIGN KEY (manager_user_id) REFERENCES users(id)
  ON DELETE SET NULL;

-- Floors
ALTER TABLE floors
  ADD CONSTRAINT fk_floors_building 
  FOREIGN KEY (building_id) REFERENCES buildings(id)
  ON DELETE CASCADE;

-- Apartments
ALTER TABLE apartments
  ADD CONSTRAINT fk_apartments_floor 
  FOREIGN KEY (floor_id) REFERENCES floors(id)
  ON DELETE CASCADE;

-- Contracts
ALTER TABLE contracts
  ADD CONSTRAINT fk_contracts_apartment 
  FOREIGN KEY (apartment_id) REFERENCES apartments(id)
  ON DELETE RESTRICT,
  ADD CONSTRAINT fk_contracts_resident 
  FOREIGN KEY (resident_id) REFERENCES residents(id)
  ON DELETE RESTRICT;

-- Contract History
ALTER TABLE contract_history
  ADD CONSTRAINT fk_contract_history_contract 
  FOREIGN KEY (contract_id) REFERENCES contracts(id)
  ON DELETE CASCADE;

-- Contract Services
ALTER TABLE contract_services
  ADD CONSTRAINT fk_contract_services_contract 
  FOREIGN KEY (contract_id) REFERENCES contracts(id)
  ON DELETE CASCADE,
  ADD CONSTRAINT fk_contract_services_service 
  FOREIGN KEY (service_id) REFERENCES services(id)
  ON DELETE RESTRICT;

-- Invoices
ALTER TABLE invoices
  ADD CONSTRAINT fk_invoices_contract 
  FOREIGN KEY (contract_id) REFERENCES contracts(id)
  ON DELETE RESTRICT;

-- Invoice Details
ALTER TABLE invoice_details
  ADD CONSTRAINT fk_invoice_details_invoice 
  FOREIGN KEY (invoice_id) REFERENCES invoices(id)
  ON DELETE CASCADE;

-- Service Usage
ALTER TABLE service_usage
  ADD CONSTRAINT fk_service_usage_contract 
  FOREIGN KEY (contract_id) REFERENCES contracts(id)
  ON DELETE CASCADE,
  ADD CONSTRAINT fk_service_usage_service 
  FOREIGN KEY (service_id) REFERENCES services(id)
  ON DELETE RESTRICT;

-- Household Members
ALTER TABLE household_members
  ADD CONSTRAINT fk_household_members_contract 
  FOREIGN KEY (contract_id) REFERENCES contracts(id)
  ON DELETE CASCADE;

-- User Buildings
ALTER TABLE user_buildings
  ADD CONSTRAINT fk_user_buildings_user 
  FOREIGN KEY (user_id) REFERENCES users(id)
  ON DELETE CASCADE,
  ADD CONSTRAINT fk_user_buildings_building 
  FOREIGN KEY (building_id) REFERENCES buildings(id)
  ON DELETE CASCADE,
  ADD CONSTRAINT fk_user_buildings_assigned_by 
  FOREIGN KEY (assigned_by) REFERENCES users(id)
  ON DELETE SET NULL;

-- =========================
-- TRIGGERS
-- =========================

DELIMITER $$

-- ✅ UPDATED: Support RENTAL/OWNERSHIP contract types
CREATE TRIGGER trg_contract_after_insert
AFTER INSERT ON contracts
FOR EACH ROW
BEGIN
  -- Update apartment status based on contract type
  IF NEW.status = 'ACTIVE' AND NEW.is_deleted = 0 THEN
    IF NEW.contract_type = 'RENTAL' THEN
      UPDATE apartments SET status = 'RENTED' WHERE id = NEW.apartment_id;
    ELSEIF NEW.contract_type = 'OWNERSHIP' THEN
      UPDATE apartments SET status = 'OWNED' WHERE id = NEW.apartment_id;
    END IF;
  END IF;

  -- Log to contract history
  INSERT INTO contract_history(contract_id, action, new_value)
  VALUES (NEW.id, 'CREATED', NEW.contract_number);
END$$

DELIMITER ;

-- =========================
-- VIEWS
-- =========================

CREATE OR REPLACE VIEW v_contract_summary AS
SELECT
  c.id AS contract_id,
  c.contract_number,
  c.contract_type,
  c.signed_date,
  c.start_date,
  c.end_date,
  c.terminated_date,
  c.deposit_amount,
  c.monthly_rent,
  c.status,
  a.id AS apartment_id,
  a.room_number AS apartment_number,
  a.area AS apartment_area,
  a.status AS apartment_status,
  f.name AS floor_name,
  f.floor_number,
  b.name AS building_name,
  r.id AS resident_id,
  r.full_name AS resident_name,
  r.phone AS resident_phone,
  r.identity_card AS resident_identity_card,
  r.email AS resident_email,
  CASE 
    WHEN c.contract_type = 'RENTAL' AND c.end_date IS NOT NULL 
    THEN DATEDIFF(c.end_date, CURDATE()) 
    ELSE NULL 
  END AS days_left,
  c.created_at,
  c.updated_at
FROM contracts c
JOIN apartments a ON c.apartment_id = a.id
JOIN floors f ON a.floor_id = f.id
JOIN buildings b ON f.building_id = b.id
JOIN residents r ON c.resident_id = r.id
WHERE c.is_deleted = 0;

-- =========================
-- INITIAL DATA
-- =========================

-- Insert default admin user
-- Password: admin123 (BCrypt hashed)
INSERT INTO users (username, password, full_name, role, is_active, is_deleted)
VALUES ('admin', '$2a$12$nUVu9lgNbiMXDYfyiogjh.Z/eW1G3w3t0rBnEci53NSQrLMwgbNaq', 'Quản trị hệ thống', 'ADMIN', 1, 0)
ON DUPLICATE KEY UPDATE username = username;

-- Insert sample services
INSERT INTO services (service_name, unit_price, unit_type, is_mandatory, is_deleted)
VALUES 
  ('Điện', 2500.00, 'kWh', 1, 0),
  ('Nước', 15000.00, 'm³', 1, 0),
  ('Internet', 200000.00, 'Tháng', 0, 0),
  ('Phí quản lý', 50000.00, 'Tháng', 1, 0),
  ('Phí gửi xe máy', 100000.00, 'Tháng', 0, 0),
  ('Phí gửi xe ô tô', 1500000.00, 'Tháng', 0, 0)
ON DUPLICATE KEY UPDATE service_name = service_name;

-- =========================
-- FINALIZE
-- =========================

COMMIT;

-- Display success message
SELECT 'Database DB_QuanLyChungCu initialized successfully!' AS Status;