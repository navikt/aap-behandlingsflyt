# Read-only SQL for investigating a behandling

Postgres. Replace `:ref` (behandling referanse UUID), `:saksnummer`, `:bid` (behandling.id), `:sid` (sak.id).
Column names verified against the Flyway migrations (`repository/src/main/resources/flyway`).

## 1. Sak and behandlinger

```sql
select s.id sak_id, s.saksnummer, s.status sak_status, s.rettighetsperiode,
       b.id, b.referanse, b.type, b.status, b.versjon, b.forrige_id, b.aarsak_til_opprettelse, b.opprettet_tid
from behandling b join sak s on s.id = b.sak_id
where s.saksnummer = :saksnummer          -- or: b.referanse = :ref
order by b.opprettet_tid;
-- type is TypeBehandling.identifikator: ae0034 Førstegangsbehandling, ae0028 Revurdering, ae0058 Klage,
-- svar-fra-andreinstans, oppfølgingsbehandling, aktivitetsplikt, aktivitetsplikt11-9, ae0203 Tilbakekreving
```

## 2. Step timeline (where it is, how it moved)

```sql
select opprettet_tid, steg, status, aktiv
from steg_historikk where behandling_id = :bid
order by opprettet_tid, id;               -- aktiv = true is the current position; TILBAKEFØRT rows = rewinds
```

## 3. Avklaringsbehov with current status and history

```sql
select a.definisjon, a.funnet_i_steg, a.krever_to_trinn, e.status, e.frist, e.venteaarsak, e.opprettet_av, e.opprettet_tid,
       left(e.begrunnelse, 80) begrunnelse
from avklaringsbehov a
join avklaringsbehov_endring e on e.avklaringsbehov_id = a.id
where a.behandling_id = :bid
order by a.id, e.opprettet_tid;           -- last row per definisjon = current status
```
`definisjon` holds the enum name (e.g. AVKLAR_SYKDOM). Kode/step/roles: `avklaringsbehov.py --definisjon NAME`.

## 4. Jobs (motor)

```sql
-- open/failed jobs for a behandling or its sak (FERDIG ones are moved to jobb_arkiv nightly)
select j.id, j.type, j.status, j.kjorbar, j.prioritet, j.neste_kjoring, j.opprettet_tid, j.behandling_id
from jobb j
where j.sak_id = :sid and j.status <> 'FERDIG'
order by j.neste_kjoring;

-- history + error of one job
select status, opprettet_tid, left(feilmelding, 2000) feilmelding
from jobb_historikk where jobb_id = :jobb_id order by opprettet_tid;

-- what ran recently for the sak (incl. archive)
select id, type, status, behandling_id, opprettet_tid from jobb where sak_id = :sid
union all
select id, type, status, behandling_id, opprettet_tid from jobb_arkiv where sak_id = :sid
order by opprettet_tid desc limit 50;

-- failure overview
select type, count(*) from jobb where status = 'FEILET' group by type order by 2 desc;
```

## 5. Vurderingsbehov and årsaker (drive rewinds and VurderingType)

```sql
select v.aarsak vurderingsbehov, v.opprettet_tid, v.oppdatert_tid, ba.aarsak aarsak_til_opprettelse, ba.opprettet_av,
       left(ba.begrunnelse, 80) begrunnelse
from vurderingsbehov v left join behandling_aarsak ba on ba.id = v.behandling_aarsak_id
where v.behandling_id = :bid
order by v.opprettet_tid;
```
A vurderingsbehov newer than a registered step's latest `steg_historikk` row rewinds the flow to that step
(`flyt.py --vurderingsbehov NAME` shows which steps).

## 6. Informasjonskrav runs (per sak; drives throttling)

```sql
select informasjonskrav, behandling_id, oppdatert, rettighetsperiode
from informasjonskrav_oppdatert where sak_id = :sid
order by oppdatert desc limit 50;
```

## 7. Documents

```sql
select id, type, status, behandling_id, referanse_type, referanse, kanal, mottatt_tid, opprettet_tid
from mottatt_dokument where sak_id = :sid order by mottatt_tid;
-- referanse_type JOURNALPOST → referanse is the journalpost id (the old journalpost column was dropped in V1.36)
```

## 8. Vilkår

```sql
select vr.behandling_id, v.type, vp.periode, vp.utfall, vp.manuell_vurdering, vp.avslagsarsak, vp.innvilgelsesarsak
from vilkar_resultat vr join vilkar v on v.resultat_id = vr.id join vilkar_periode vp on vp.vilkar_id = v.id
where vr.behandling_id = :bid and vr.aktiv
order by v.type, vp.periode;
```

## 9. (Write — last resort) re-queue processing safely

Prefer the drift API / motor-api. If many behandlinger must be reprocessed (see `docs/playbook.md`):
```sql
insert into jobb (status, type, sak_id, behandling_id, neste_kjoring, opprettet_tid, parameters, payload, kjorbar, prioritet)
select 'KLAR', 'flyt.prosesserBehandling', b.sak_id, b.id, now(), now(), null, null,
       false,            -- let the scheduler promote: respects ordering and the unique index UX_JOBB_EKSKLUSIV_AKTIV
       200               -- Prioritet.LAV: let saksbehandler-triggered work go first
from behandling b
where /* your selection */ false
  and not exists (select 1 from jobb j where j.behandling_id = b.id and j.type = 'flyt.prosesserBehandling'
                  and j.status in ('KLAR', 'FEILET'));
```
