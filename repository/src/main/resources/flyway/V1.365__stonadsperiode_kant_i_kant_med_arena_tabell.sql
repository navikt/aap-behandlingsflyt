CREATE TABLE stonadsperiode_kant_i_kant_med_arena
(
    id               BIGSERIAL    NOT NULL PRIMARY KEY,
    arena_saksnummer TEXT         NOT NULL,
    status           TEXT         NOT NULL,
    maksdato         DATE         NOT NULL,
    vedtak_til       DATE,
    vedtak_id        BIGINT       NOT NULL,
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE stonadsperiode_vurdering
    ADD COLUMN kant_i_kant_med_arena_id BIGINT REFERENCES stonadsperiode_kant_i_kant_med_arena (id);

CREATE INDEX idx_stonadsperiode_vurdering_kant_i_kant_med_arena_id
    ON stonadsperiode_vurdering (kant_i_kant_med_arena_id);

-- Fyller inn Arena-data for eksisterende kant i kant-vurderinger fra aktivt arena_maksdato-grunnlag
-- i behandlingen der vurderingen ble gjort.
do $$
declare
    rec record;
    ny_id bigint;
begin
    for rec in select v.id, a.arena_saksnummer, a.status, a.maksdato, a.vedtak_til, a.vedtak_id
               from stonadsperiode_vurdering v
                        join arena_maksdato_grunnlag a
                             on a.behandling_id = v.vurdert_i_behandling and a.aktiv
               where v.kant_i_kant_med_arena
                 and a.arena_saksnummer is not null
                 and a.status is not null
                 and a.maksdato is not null
                 and a.vedtak_id is not null
    loop
        insert into stonadsperiode_kant_i_kant_med_arena (arena_saksnummer, status, maksdato, vedtak_til, vedtak_id)
        values (rec.arena_saksnummer, rec.status, rec.maksdato, rec.vedtak_til, rec.vedtak_id)
        returning id into ny_id;

        update stonadsperiode_vurdering set kant_i_kant_med_arena_id = ny_id where id = rec.id;
    end loop;

    if exists (select 1
               from stonadsperiode_vurdering
               where kant_i_kant_med_arena
                 and kant_i_kant_med_arena_id is null) then
        raise exception 'Fant kant i kant-vurderinger uten Arena-maksdato å fylle inn fra';
    end if;
end $$;
