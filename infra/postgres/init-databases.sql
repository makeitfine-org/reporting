-- Create databases and users for all microservices
-- This script runs once when the postgres container is first created.
-- The default 'reporting' database is already created by POSTGRES_DB env var.

CREATE
USER customers WITH PASSWORD 'customers';
CREATE
DATABASE customers OWNER customers;

CREATE
USER products WITH PASSWORD 'products';
CREATE
DATABASE products OWNER products;

CREATE
USER transactions WITH PASSWORD 'transactions';
CREATE
DATABASE transactions OWNER transactions;

CREATE
USER notifications WITH PASSWORD 'notifications';
CREATE
DATABASE notifications OWNER notifications;
