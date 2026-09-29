CREATE INDEX ix_unit_occupancies_customer ON unit_occupancies(customer_id, end_date);
CREATE INDEX ix_unit_occupancies_unit_active ON unit_occupancies(unit_id, end_date, is_primary);
CREATE INDEX ix_customers_name ON customers(full_name);
