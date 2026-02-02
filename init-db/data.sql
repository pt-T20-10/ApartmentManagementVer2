-- =========================
-- DATABASE
-- =========================
CREATE DATABASE IF NOT EXISTS DB_QuanLyChungCu
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE DB_QuanLyChungCu;

SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET time_zone = '+00:00';

-- =========================
-- TABLES (NO FK, NO TRIGGER)
-- =========================

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  role VARCHAR(20) DEFAULT 'STAFF',
  is_active TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE TABLE buildings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  address VARCHAR(255),
  manager_name VARCHAR(100),
  description TEXT,
  is_deleted TINYINT(1) DEFAULT 0,
  status VARCHAR(50) DEFAULT 'Đang hoạt động',
  manager_user_id BIGINT
) ENGINE=InnoDB;

CREATE TABLE floors (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  building_id BIGINT NOT NULL,
  floor_number INT NOT NULL,
  name VARCHAR(50),
  is_deleted TINYINT(1) DEFAULT 0,
  status VARCHAR(50) DEFAULT 'Đang hoạt động'
) ENGINE=InnoDB;

CREATE TABLE apartments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  floor_id BIGINT NOT NULL,
  room_number VARCHAR(20) NOT NULL,
  area DOUBLE,
  status VARCHAR(20) DEFAULT 'AVAILABLE',
  description TEXT,
  is_deleted TINYINT(1) DEFAULT 0,
  apartment_type VARCHAR(50) DEFAULT 'Standard',
  bedroom_count INT DEFAULT 1,
  bathroom_count INT DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE residents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(100),
  identity_card VARCHAR(20) NOT NULL,
  gender VARCHAR(10),
  dob DATE,
  hometown VARCHAR(255),
  is_deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE contracts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_number VARCHAR(50) UNIQUE,
  apartment_id BIGINT NOT NULL,
  resident_id BIGINT NOT NULL,
  contract_type VARCHAR(20) DEFAULT 'RENTAL',
  signed_date DATE,
  start_date DATE,
  end_date DATE,
  terminated_date DATE,
  notes TEXT,
  deposit_amount DECIMAL(15,2),
  monthly_rent DECIMAL(15,2) DEFAULT 0,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  is_deleted TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE contract_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  old_value TEXT,
  new_value TEXT,
  old_end_date DATE,
  new_end_date DATE,
  reason TEXT,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE services (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service_name VARCHAR(100) NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  unit_type VARCHAR(20),
  is_mandatory TINYINT(1) DEFAULT 0,
  is_deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE contract_services (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  applied_date DATE NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  is_active TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_contract_service (contract_id, service_id, is_active)
) ENGINE=InnoDB;

CREATE TABLE invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  month INT NOT NULL,
  year INT NOT NULL,
  total_amount DECIMAL(15,2) DEFAULT 0,
  status VARCHAR(20) DEFAULT 'UNPAID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  payment_date TIMESTAMP NULL,
  is_deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE invoice_details (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  service_name VARCHAR(100),
  unit_price DECIMAL(15,2),
  quantity DOUBLE,
  amount DECIMAL(15,2)
) ENGINE=InnoDB;

CREATE TABLE service_usage (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  month INT NOT NULL,
  year INT NOT NULL,
  old_index DOUBLE DEFAULT 0,
  new_index DOUBLE DEFAULT 0,
  actual_usage DOUBLE
) ENGINE=InnoDB;

CREATE TABLE household_members (
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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =========================
-- FOREIGN KEYS
-- =========================
ALTER TABLE buildings ADD FOREIGN KEY (manager_user_id) REFERENCES users(id);
ALTER TABLE floors ADD FOREIGN KEY (building_id) REFERENCES buildings(id);
ALTER TABLE apartments ADD FOREIGN KEY (floor_id) REFERENCES floors(id);
ALTER TABLE contracts ADD FOREIGN KEY (apartment_id) REFERENCES apartments(id);
ALTER TABLE contracts ADD FOREIGN KEY (resident_id) REFERENCES residents(id);
ALTER TABLE contract_history ADD FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE;
ALTER TABLE contract_services ADD FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE;
ALTER TABLE contract_services ADD FOREIGN KEY (service_id) REFERENCES services(id);
ALTER TABLE invoices ADD FOREIGN KEY (contract_id) REFERENCES contracts(id);
ALTER TABLE invoice_details ADD FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE;
ALTER TABLE service_usage ADD FOREIGN KEY (contract_id) REFERENCES contracts(id);
ALTER TABLE service_usage ADD FOREIGN KEY (service_id) REFERENCES services(id);
ALTER TABLE household_members ADD FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE;

-- =========================
-- TRIGGERS
-- =========================
DELIMITER $$

CREATE TRIGGER trg_contract_after_insert
AFTER INSERT ON contracts
FOR EACH ROW
BEGIN
  IF NEW.status = 'ACTIVE' AND NEW.is_deleted = 0 THEN
    UPDATE apartments SET status = 'RENTED' WHERE id = NEW.apartment_id;
  END IF;

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
  c.status,
  a.room_number AS apartment_number,
  a.area AS apartment_area,
  f.name AS floor_name,
  b.name AS building_name,
  r.id AS resident_id,
  r.full_name AS resident_name,
  r.phone AS resident_phone,
  r.identity_card AS resident_identity_card,
  r.email AS resident_email,
  DATEDIFF(c.end_date, CURDATE()) AS days_left,
  c.created_at,
  c.updated_at
FROM contracts c
JOIN apartments a ON c.apartment_id = a.id
JOIN floors f ON a.floor_id = f.id
JOIN buildings b ON f.building_id = b.id
JOIN residents r ON c.resident_id = r.id
WHERE c.is_deleted = 0;
