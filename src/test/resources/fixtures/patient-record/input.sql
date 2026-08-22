-- Exemple de schéma DDL pour tester
CREATE TABLE patient_record (
    id          UUID        NOT NULL,
    ipp         VARCHAR(20) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    first_name  VARCHAR(255) NOT NULL,
    birth_date  DATE,
    gender      VARCHAR(10),
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP,
    CONSTRAINT pk_patient PRIMARY KEY (id)
);

CREATE TABLE encounter (
    id              UUID        NOT NULL,
    patient_id      UUID        NOT NULL,
    encounter_date  TIMESTAMP   NOT NULL,
    discharge_date  TIMESTAMP,
    status          VARCHAR(50) NOT NULL,
    note            TEXT,
    CONSTRAINT pk_encounter PRIMARY KEY (id)
);
