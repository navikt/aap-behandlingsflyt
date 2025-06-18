ALTER TABLE samordning_vurderinger
    ALTER COLUMN MAKSDATO_ENDELIG DROP DEFAULT;

UPDATE samordning_vurderinger SET maksdato_endelig = true where maksdato is null and maksdato_endelig = false;