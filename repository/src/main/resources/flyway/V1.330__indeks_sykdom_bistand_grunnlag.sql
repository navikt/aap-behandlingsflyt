-- Indeks for SykdomRepository::hentSykdomsvurderingerPåTidspunkt
-- som nå gjør sekvensiell scan.
--
--  SELECT sg.sykdom_vurderinger_id
--  FROM sykdom_grunnlag sg
--  WHERE sg.behandling_id = ?
--    AND sg.opprettet_tid <= ?
--  ORDER BY sg.opprettet_tid DESC
--  LIMIT 1

create index idx_sykdom_grunnlag_behandling_id_opprettet_tid
    on sykdom_grunnlag (behandling_id, opprettet_tid desc);

-- Nøyaktig samme situasjon for bistand_grunnlag.
create index idx_bistand_grunnlag_behandling_id_opprettet_tid
    on bistand_grunnlag (behandling_id, opprettet_tid desc);
