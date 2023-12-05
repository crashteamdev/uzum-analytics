--liquibase formatted sql
--changeset vitaxa:add-product-restriction-info
ALTER TABLE uzum.product ADD COLUMN IF NOT EXISTS restriction Int16 DEFAULT 0;
