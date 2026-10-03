ALTER TABLE unit_service_assignments ADD tariff_id BIGINT NULL;

ALTER TABLE unit_service_assignments
    ADD CONSTRAINT fk_unit_service_tariff
    FOREIGN KEY (tariff_id) REFERENCES service_tariffs(id);

CREATE INDEX ix_unit_service_tariff ON unit_service_assignments(tariff_id);

