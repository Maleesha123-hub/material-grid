-- Make assignment_date nullable in vehicle_licenses
ALTER TABLE vehicle_licenses MODIFY COLUMN assignment_date DATE NULL;
