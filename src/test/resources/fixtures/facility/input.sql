-- Fixture pour le ticket #10 : FK composite (corrélation positionnelle colonne
-- locale <-> colonne cible) et colonne locale participant à plusieurs FK distinctes.
CREATE TABLE region (
    country_code  VARCHAR(2)   NOT NULL,
    region_code   VARCHAR(10)  NOT NULL,
    name          VARCHAR(255),
    CONSTRAINT pk_region PRIMARY KEY (country_code, region_code)
);

CREATE TABLE person (
    id    UUID NOT NULL,
    name  VARCHAR(255),
    CONSTRAINT pk_person PRIMARY KEY (id)
);

CREATE TABLE organization (
    id    UUID NOT NULL,
    name  VARCHAR(255),
    CONSTRAINT pk_organization PRIMARY KEY (id)
);

CREATE TABLE facility (
    id              UUID        NOT NULL,
    country_code    VARCHAR(2)  NOT NULL,
    region_code     VARCHAR(10) NOT NULL,
    responsible_id  UUID,
    CONSTRAINT pk_facility PRIMARY KEY (id),
    CONSTRAINT fk_facility_region FOREIGN KEY (country_code, region_code) REFERENCES region(country_code, region_code),
    CONSTRAINT fk_facility_person FOREIGN KEY (responsible_id) REFERENCES person(id),
    CONSTRAINT fk_facility_organization FOREIGN KEY (responsible_id) REFERENCES organization(id)
);
