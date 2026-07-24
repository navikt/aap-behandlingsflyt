-- Indeks for hentHvisEksisterer, hentYtelseIderPÅBehandlingId, deaktiverGrunnlag og slett.
-- Partiel indeks dekker kun aktive rader og holder seg liten over tid.
CREATE INDEX idx_andre_ytelser_grunnlag_behandling_aktiv
    ON ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG (behandling_id)
    WHERE aktiv = TRUE;

-- Dekker JOIN i hentHvisEksisterer og slett
CREATE INDEX idx_annen_ytelse_soknad_andre_ytelser_id
    ON ANNEN_YTELSE_OPPGITT_I_SOKNAD (andre_ytelser_id);

-- Dekker hentAlleGrunnlagIdPåAndreYtelseId og slett.
CREATE INDEX idx_andre_ytelser_grunnlag_ytelser_id
    ON ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG (andre_ytelser_id);
