CREATE TABLE anonymous_constraints (
    id UUID NOT NULL,
    code VARCHAR(10),
    a INT,
    b INT,
    CONSTRAINT pk_anon PRIMARY KEY (id),
    UNIQUE (code),
    CHECK (a > 0),
    CHECK (b > 0),
    CONSTRAINT "___" CHECK (id IS NOT NULL),
    UNIQUE (a, b)
);
