# Skrivebeskyttet SQL for å undersøke en behandling

Postgres. Erstatt `:ref` (behandlingsreferanse UUID), `:saksnummer`, `:bid` (behandling.id), `:sid` (sak.id).
Kolonnenavnene er kontrollert mot Flyway-migreringene (`repository/src/main/resources/flyway`).

## 1. Sak og behandlinger

```sql
select s.id sak_id, s.saksnummer, s.status sak_status, s.rettighetsperiode,
       b.id, b.referanse, b.type, b.status, b.versjon, b.forrige_id, b.aarsak_til_opprettelse, b.opprettet_tid
from behandling b join sak s on s.id = b.sak_id
where s.saksnummer = :saksnummer          -- eller: b.referanse = :ref
order by b.opprettet_tid;
-- type er TypeBehandling.identifikator: ae0034 Førstegangsbehandling, ae0028 Revurdering, ae0058 Klage,
-- svar-fra-andreinstans, oppfølgingsbehandling, aktivitetsplikt, aktivitetsplikt11-9, ae0203 Tilbakekreving
```

## 2. Stegtidslinje (hvor den er, hvordan den flyttet seg)

```sql
select opprettet_tid, steg, status, aktiv
from steg_historikk where behandling_id = :bid
order by opprettet_tid, id;               -- aktiv = true er gjeldende posisjon; TILBAKEFØRT-rader = tilbakeføringer
```

## 3. Avklaringsbehov med gjeldende status og historikk

```sql
select a.definisjon, a.funnet_i_steg, a.krever_to_trinn, e.status, e.frist, e.venteaarsak, e.opprettet_av, e.opprettet_tid,
       left(e.begrunnelse, 80) begrunnelse
from avklaringsbehov a
join avklaringsbehov_endring e on e.avklaringsbehov_id = a.id
where a.behandling_id = :bid
order by a.id, e.opprettet_tid;           -- siste rad per definisjon = gjeldende status
```
`definisjon` inneholder enum-navnet (for eksempel AVKLAR_SYKDOM). Kode/steg/roller: `avklaringsbehov.py --definisjon NAME`.

## 4. Jobber (motor)

```sql
-- åpne/feilede jobber for en behandling eller saken dens (FERDIG-jobber flyttes til jobb_arkiv hver natt)
select j.id, j.type, j.status, j.kjorbar, j.prioritet, j.neste_kjoring, j.opprettet_tid, j.behandling_id
from jobb j
where j.sak_id = :sid and j.status <> 'FERDIG'
order by j.neste_kjoring;

-- historikk og feil for én jobb
select status, opprettet_tid, left(feilmelding, 2000) feilmelding
from jobb_historikk where jobb_id = :jobb_id order by opprettet_tid;

-- nylig kjørte jobber for saken (inkludert arkivet)
select id, type, status, behandling_id, opprettet_tid from jobb where sak_id = :sid
union all
select id, type, status, behandling_id, opprettet_tid from jobb_arkiv where sak_id = :sid
order by opprettet_tid desc limit 50;

-- oversikt over feil
select type, count(*) from jobb where status = 'FEILET' group by type order by 2 desc;
```

## 5. Vurderingsbehov og årsaker (utløser tilbakeføringer og VurderingType)

```sql
select v.aarsak vurderingsbehov, v.opprettet_tid, v.oppdatert_tid, ba.aarsak aarsak_til_opprettelse, ba.opprettet_av,
       left(ba.begrunnelse, 80) begrunnelse
from vurderingsbehov v left join behandling_aarsak ba on ba.id = v.behandling_aarsak_id
where v.behandling_id = :bid
order by v.opprettet_tid;
```
Et vurderingsbehov som er nyere enn det nyeste `steg_historikk`-elementet for et registrert steg, fører til at flyten går tilbake til dette steget
(`flyt.py --vurderingsbehov NAME` viser hvilke steg).

## 6. Kjøringer av informasjonskrav (per sak; styrer begrensning av kjøringer)

```sql
select informasjonskrav, behandling_id, oppdatert, rettighetsperiode
from informasjonskrav_oppdatert where sak_id = :sid
order by oppdatert desc limit 50;
```

## 7. Dokumenter

```sql
select id, type, status, behandling_id, referanse_type, referanse, kanal, mottatt_tid, opprettet_tid
from mottatt_dokument where sak_id = :sid order by mottatt_tid;
-- referanse_type JOURNALPOST → referanse er journalpost-ID-en (den gamle journalpost-kolonnen ble fjernet i V1.36)
```

## 8. Vilkår

```sql
select vr.behandling_id, v.type, vp.periode, vp.utfall, vp.manuell_vurdering, vp.avslagsarsak, vp.innvilgelsesarsak
from vilkar_resultat vr join vilkar v on v.resultat_id = vr.id join vilkar_periode vp on vp.vilkar_id = v.id
where vr.behandling_id = :bid and vr.aktiv
order by v.type, vp.periode;
```

## 9. (Skriving, siste utvei) legg prosessering trygt i kø på nytt

Bruk helst drift-API-et eller motor-API-et. Hvis mange behandlinger må prosesseres på nytt (se `docs/playbook.md`):
```sql
insert into jobb (status, type, sak_id, behandling_id, neste_kjoring, opprettet_tid, parameters, payload, kjorbar, prioritet)
select 'KLAR', 'flyt.prosesserBehandling', b.sak_id, b.id, now(), now(), null, null,
       false,            -- la planleggeren prioritere: følger rekkefølgen og den unike indeksen UX_JOBB_EKSKLUSIV_AKTIV
       200               -- Prioritet.LAV: la arbeid utløst av saksbehandleren kjøre først
from behandling b
where /* ditt utvalg */ false
  and not exists (select 1 from jobb j where j.behandling_id = b.id and j.type = 'flyt.prosesserBehandling'
                  and j.status in ('KLAR', 'FEILET'));
```
