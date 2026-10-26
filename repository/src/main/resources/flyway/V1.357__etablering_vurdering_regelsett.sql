ALTER TABLE etablering_egen_virksomhet_vurdering
    ADD COLUMN regelsett VARCHAR(20) NOT NULL DEFAULT 'LEGACY'
        CHECK (regelsett IN ('LEGACY', 'FASEBASERT'));
