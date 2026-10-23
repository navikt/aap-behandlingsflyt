# Module docs

## Playbook

## Hvordan avbryte behandlinger som ble feilaktig opprettet

19 oktober 2026 ble det oppdaget at pga en endring i `OppdagEndretInformasjonskravJobbUtfører.kt` (https://github.com/navikt/aap-behandlingsflyt/pull/3659), så ble det opprettet veldig mange revurderinger hos NAY. Ny løsning ble å gjøre registerkall direkte i en annen jobb (https://github.com/navikt/aap-behandlingsflyt/pull/3674/changes/00613f7d4d417c646b34884a4e3034f9e82fc630).

Formålet med endringen var å få oppdatert informasjon om TP-ordninger, men konsekvensen var ikke tiltenkt.

Vi måtte finne en måte å avbryte alle disse behandlingene på.

De hadde alle blitt opprettet med årsak `ENDRING_I_REGISTERDATA` og med vurderingsbehov `REVURDER_SAMORDNING_TJENESTEPENSJON`.

Det var ikke nok å endre kun på `TjenestepensjonRefusjonskravSteg`, ettersom behandlingen gikk til både beslutter og brev-steget etterpå. Totalt var det disse endringene vi måtte gjøre: (commit `4d31607ea99342defb108b0b9b5eff7865b8e52b`)

```diff
commit 4d31607ea99342defb108b0b9b5eff7865b8e52b
Author: Fredrik Meyer <fredrik.meyer@nav.no>
Date:   Tue Oct 20 08:46:55 2026 +0200

    AAP-BAU Avbryt behandlinger som feilaktig ble opprettet pga informasjonskrav

diff --git a/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt b/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt
index ccc0f97aa0..7843e0b1bd 100644
--- a/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt
+++ b/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt
@@ -1,6 +1,8 @@
 package no.nav.aap.behandlingsflyt.forretningsflyt.steg
 
+import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
 import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
+import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
 import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
 import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
 import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
@@ -11,6 +13,8 @@ import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
 import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
 import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
 import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
+import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
+import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
 import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
 import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
 import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
@@ -22,12 +26,16 @@ class TjenestepensjonRefusjonskravSteg private constructor(
     private val avklaringsbehovService: AvklaringsbehovService,
     private val tjenestePensjonRepository: TjenestePensjonRepository,
     private val tidligereVurderinger: TidligereVurderinger,
+    private val avklaringsbehovRepository: AvklaringsbehovOperasjonerRepository,
+    private val behandlingRepository: BehandlingRepository,
 ) : BehandlingSteg {
     constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
         tjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide(),
         avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
         tjenestePensjonRepository = repositoryProvider.provide(),
-        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider)
+        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
+        avklaringsbehovRepository = repositoryProvider.provide(),
+        behandlingRepository = repositoryProvider.provide(),
     )
 
     override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
@@ -38,7 +46,19 @@ class TjenestepensjonRefusjonskravSteg private constructor(
                     VurderingType.FØRSTEGANGSBEHANDLING,
                     VurderingType.REVURDERING,
                     VurderingType.MIGERING_FRA_ARENA -> {
+                        val avklaringsbehovene =
+                            Avklaringsbehovene(avklaringsbehovRepository, kontekst.behandlingId).alle()
+                        val behandling = behandlingRepository.hent(kontekst.behandlingId)
+                        val vurderingsbehov = behandling.vurderingsbehov().map { it.type }
+                        val årsakTilOpprettelse = behandling.årsakTilOpprettelse
+
+                        val bleOpprettetKunPgaInformasjonkrav =
+                            avklaringsbehovene.size == 1 && avklaringsbehovene.single().definisjon == Definisjon.SAMORDNING_REFUSJONS_KRAV
+                                    && årsakTilOpprettelse == ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
+                                    && vurderingsbehov.size == 1 && vurderingsbehov.single() == Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON
+
                         when {
+                            bleOpprettetKunPgaInformasjonkrav -> false
                             tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                             kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty() -> true
                             Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON in kontekst.vurderingsbehovRelevanteForSteg -> true

```

og

```diff
commit 7dfba3b1a7825f8c589dfe63084e26899ef7f301
Author: Fredrik Meyer <fredrik.meyer@nav.no>
Date:   Tue Oct 20 09:25:23 2026 +0200

    AAP-BAU Eksplitt avbryt pga frivillig

diff --git a/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt b/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt
index 7843e0b1bd..9d14d36a05 100644
--- a/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt
+++ b/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/TjenestepensjonRefusjonskravSteg.kt
@@ -20,6 +20,7 @@ import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
 import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
 import no.nav.aap.komponenter.gateway.GatewayProvider
 import no.nav.aap.lookup.repository.RepositoryProvider
+import org.slf4j.LoggerFactory
 
 class TjenestepensjonRefusjonskravSteg private constructor(
     private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
@@ -38,6 +39,8 @@ class TjenestepensjonRefusjonskravSteg private constructor(
         behandlingRepository = repositoryProvider.provide(),
     )
 
+    private val log = LoggerFactory.getLogger(TjenestepensjonRefusjonskravSteg::class.java)
+
     override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
         avklaringsbehovService.oppdaterAvklaringsbehov(
             definisjon = Definisjon.SAMORDNING_REFUSJONS_KRAV,
@@ -52,11 +55,17 @@ class TjenestepensjonRefusjonskravSteg private constructor(
                         val vurderingsbehov = behandling.vurderingsbehov().map { it.type }
                         val årsakTilOpprettelse = behandling.årsakTilOpprettelse
 
+                        log.info("Avklaringsbehovene: $avklaringsbehovene. Vurderingsbehovene: $vurderingsbehov. Årsak til opprettelse: $årsakTilOpprettelse.")
+
                         val bleOpprettetKunPgaInformasjonkrav =
                             avklaringsbehovene.size == 1 && avklaringsbehovene.single().definisjon == Definisjon.SAMORDNING_REFUSJONS_KRAV
                                     && årsakTilOpprettelse == ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
                                     && vurderingsbehov.size == 1 && vurderingsbehov.single() == Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON
 
+                        if (bleOpprettetKunPgaInformasjonkrav) {
+                            Avklaringsbehovene(avklaringsbehovRepository, kontekst.behandlingId).avbryt(Definisjon.SAMORDNING_REFUSJONS_KRAV)
+                        }
+
                         when {
                             bleOpprettetKunPgaInformasjonkrav -> false
                             tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false

```

Brevsteget måtte også avbrytes:

```diff
commit ad4158f90b36d968f68d726a7add5308eae709ed
Author: Fredrik Meyer <fredrik.meyer@nav.no>
Date:   Tue Oct 20 10:28:57 2026 +0200

    AAP-BAU Også avbryt brevsteget

diff --git a/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/MeldingOmVedtakBrevSteg.kt b/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/MeldingOmVedtakBrevSteg.kt
index 93fd421e14..94b130cae1 100644
--- a/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/MeldingOmVedtakBrevSteg.kt
+++ b/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/MeldingOmVedtakBrevSteg.kt
@@ -1,5 +1,6 @@
 package no.nav.aap.behandlingsflyt.forretningsflyt.steg
 
+import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
 import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingService
 import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
 import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
@@ -22,8 +23,10 @@ import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
 import no.nav.aap.behandlingsflyt.prosessering.OpprettJobbForTriggBarnetilleggSatsJobbUtfører
 import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
 import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
+import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
 import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
 import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
+import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
 import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
 import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
 import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
@@ -46,6 +49,7 @@ class MeldingOmVedtakBrevSteg(
     private val avbrytAktivitetspliktbehandlingService: AvbrytAktivitetspliktbehandlingService,
     private val avklaringsbehovService: AvklaringsbehovService,
     private val avklaringsbehovRepository: AvklaringsbehovRepository,
+    private val avklaringsbehovOperasjonerRepository: AvklaringsbehovOperasjonerRepository,
     private val unleashGateway: UnleashGateway,
 ) : BehandlingSteg {
     constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
@@ -57,6 +61,7 @@ class MeldingOmVedtakBrevSteg(
         avbrytAktivitetspliktbehandlingService = AvbrytAktivitetspliktbehandlingService(repositoryProvider),
         avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
         avklaringsbehovRepository = repositoryProvider.provide(),
+        avklaringsbehovOperasjonerRepository = repositoryProvider.provide(),
         unleashGateway = gatewayProvider.provide(),
     )
 
@@ -68,6 +73,30 @@ class MeldingOmVedtakBrevSteg(
         log.info("Har brevbehov " + brevBehov?.typeBrev)
         val harBestillingOmVedtakBrev = brevbestillingService.harBestillingOmVedtak(kontekst.behandlingId)
 
+        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovOperasjonerRepository, kontekst.behandlingId)
+        val behandling = behandlingRepository.hent(kontekst.behandlingId)
+        val vurderingsbehov = behandling.vurderingsbehov().map { it.type }
+        val årsakTilOpprettelse = behandling.årsakTilOpprettelse
+
+        val refKravStegAvbrutt =
+            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SAMORDNING_REFUSJONS_KRAV)?.status() == Status.AVBRUTT
+
+        val bleOpprettetKunPgaInformasjonkrav = årsakTilOpprettelse == ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
+                && vurderingsbehov.size == 1 && vurderingsbehov.single() == Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON
+
+        if (bleOpprettetKunPgaInformasjonkrav && refKravStegAvbrutt) {
+            if (avklaringsbehovene.alle().map { it.definisjon }.contains(Definisjon.SKRIV_VEDTAKSBREV))
+                avklaringsbehovene.avbryt(Definisjon.SKRIV_VEDTAKSBREV)
+            if (brevbestillingService.harBestillingOmVedtak(kontekst.behandlingId)) {
+                val bestillinger =
+                    brevbestillingService.hentBestillinger(kontekst.behandlingId, TypeBrev.VEDTAK_INNVILGELSE)
+                bestillinger.forEach {
+                    brevbestillingService.avbryt(kontekst.behandlingId, it.referanse)
+                }
+            }
+            return Fullført
+        }
+
         listOf(
             Definisjon.SKRIV_VEDTAKSBREV,
             Definisjon.SKRIV_VEDTAKSBREV_SAKSBEHANDLER
```

(også commit `cfe9cf04358139fdd06e8061f065b5136ce17c1b` som fikset kompileringsfeil i tester)

Når dette var deployet, kunne vi reprosessere behandlinger for at stegene skulle bli kjørt igjen. Vi verifiserte først i Paw Patrol på et par eksempler, deretter køet vi jobber direkte:

```sql
insert into jobb (status, type, sak_id, behandling_id, neste_kjoring, opprettet_tid, parameters,
                  payload, kjorbar, prioritet)
select 'KLAR',
       'flyt.prosesserBehandling',
       s.id,
       b.id,
       now(),
       now(),
       null,
       null,
       true,
       100
from sak s
         join behandling b on s.id = b.sak_id
         join vurderingsbehov on b.id = vurderingsbehov.behandling_id
where aarsak = 'REVURDER_SAMORDNING_TJENESTEPENSJON'
  and b.status != 'AVSLUTTET'
  and (select count(*) from avklaringsbehov a where a.behandling_id = b.id) = 1;
```

Dette førte til opprettelse av ca 1000 jobber, som tok ca 30 min å fullføre. (i ettertid burde vi nok satt `kjorbar` til `false`, slik at ikke alle jobbene ble klare på én gang, dette førte til noe treghet i Kelvin).

Etter at disse var kjørt, var oppryddingen ferdig.


Etterpå ble den midlertidige koden ryddet opp (commit `1c1d7e183c4dd6cb5fb4e95769b1b2d9d2cbd7c4`).
