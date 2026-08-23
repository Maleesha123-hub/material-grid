CREATE TABLE code_sequences (
    sequence_name VARCHAR(50) NOT NULL PRIMARY KEY,
    next_value    BIGINT      NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed rows for every business code type. The generator (CodeGeneratorService)
-- only ever SELECTs ... FOR UPDATE and UPDATEs an existing row - it never
-- inserts - so the row must already exist before the first code is issued.
INSERT INTO code_sequences (sequence_name, next_value) VALUES ('ROUTE_CODE', 1);
INSERT INTO code_sequences (sequence_name, next_value) VALUES ('LICENSE_CODE', 1);
