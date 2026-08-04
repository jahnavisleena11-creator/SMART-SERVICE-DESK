-- Script: alter_sla_status.sql
-- Purpose: Fix MySQL 'sla_status' column so it accepts the Java enum values used by the application.
-- Two options provided. Run one of them in your MySQL console (as a user with ALTER TABLE privileges).

-- OPTION A (preferred): Convert/modify the column to an ENUM containing the exact Java enum values.
-- This keeps the database as an ENUM but ensures the allowed values match the Java enum.
-- Replace `tickets` with your actual table name if different, and `mits` with your database name if needed.

ALTER TABLE `tickets`
  MODIFY COLUMN `sla_status` ENUM('ON_TIME', 'NEAR_BREACH', 'BREACHED') NOT NULL DEFAULT 'ON_TIME';

-- OPTION B (safer fallback): Convert the column to a VARCHAR so the application can write arbitrary enum names.
-- This avoids ENUM-value mismatches entirely and is easier to maintain for a college project.

-- ALTER TABLE `tickets`
--   MODIFY COLUMN `sla_status` VARCHAR(50) NOT NULL DEFAULT 'ON_TIME';

-- Notes:
-- 1) If your column currently uses different enum labels (for example 'ON-TIME' or 'ONTIME'),
--    you may need to update existing rows before running OPTION A. Example:
--    UPDATE `tickets` SET `sla_status` = 'ON_TIME' WHERE `sla_status` IN ('ONTIME','ON-TIME');
-- 2) Back up your data before running ALTER TABLE on production databases.
-- 3) With `spring.jpa.hibernate.ddl-auto=update` Hibernate may not alter existing ENUM definitions in MySQL,
--    so running this script manually is the reliable fix.

-- To run (example):
-- mysql -u root -p
-- USE mits;
-- SOURCE path/to/alter_sla_status.sql;
