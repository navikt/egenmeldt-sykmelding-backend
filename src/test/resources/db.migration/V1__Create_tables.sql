CREATE TABLE egenmeldt_sykmelding (
    id UUID PRIMARY KEY,
    pasientfnr CHAR(11) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    receivedsykmelding jsonb NOT NULL
);