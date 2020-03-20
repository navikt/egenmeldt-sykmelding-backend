CREATE TABLE egenmeldt_sykmelding (
    id UUID PRIMARY KEY,
    pasientfnr CHAR(11) NOT NULL,
    fom DATE NOT NULL,
    tom DATE NOT NULL,
    arbeidsforhold JSONB
);