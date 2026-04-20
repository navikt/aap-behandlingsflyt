package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SammenlignetSegment
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykdomsvilkårUtenVissVarighet
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.diff
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class SykdomsvurderingMigrering(
    val dataSource: DataSource,
    val repositoryRegistry: RepositoryRegistry,
    val gatewayProvider: GatewayProvider
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun migrer() {
        log.info("Starter migrering av sykdomsvurdering (nye nedsettelsesfelt)")
        var ferdig = false
        var antallMigreringer = 0
        var sisteMigrerte = 0L

        while (!ferdig) {
            dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sykdomsvurderingMigreringService =
                    SykdomsvurderingMigreringService(repositoryProvider, gatewayProvider)

                val behandlingIder =
                    sykdomsvurderingMigreringService.hentNesteBehandlingIdMedUmigrerteSykdomsvurderinger(
                        sisteMigrerte
                    )

                if (behandlingIder.isEmpty()) {
                    ferdig = true
                    log.info("Ferdig med migrering av sykdomsvurdering. Totalt $antallMigreringer behandlinger migrert.")
                    return@transaction
                }

                val behandlingId = behandlingIder.first()
                try {
                    sykdomsvurderingMigreringService.migrerBehandling(behandlingId)
                } catch (e: Exception) {
                    log.error("Migrering feilet for behandling $behandlingId. Avbryter migrering.", e)
                    ferdig = true
                }
                antallMigreringer += 1
                sisteMigrerte = behandlingId.id

                if (antallMigreringer % 10 == 0) {
                    log.info("Pågående migrering av sykdomsvurdering, $antallMigreringer behandlinger migrert")
                }
            }
        }
    }
}

class SykdomsvurderingMigreringService(
    val sykdomRepository: SykdomRepository,
    val vilkårsresultatRepository: VilkårsresultatRepository,
    val behandlingRepository: BehandlingRepository,
    val bistandRepository: BistandRepository,
    val sykepengerErstatningRepository: SykepengerErstatningRepository,
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val WHITELISTEDE_BEHANDLING_ID_ER_PRODUKSJON = listOf<Long>(72, 73, 75, 81, 184, 187, 188, 189, 192, 110)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sykdomRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
    )

    fun hentNesteBehandlingIdMedUmigrerteSykdomsvurderinger(sisteBehandlingId: Long): List<BehandlingId> {
        return sykdomRepository.hentBehandlingIderMedUmigrerteSykdomsvurderinger(sisteBehandlingId)
    }

    fun migrerBehandling(behandlingId: BehandlingId) {
        // Hent sykdomsvurderinger med ID og oppdater
        val sykdomsvurderingerMedId = sykdomRepository.hentSykdomsvurderingMedId(behandlingId)

        if (sykdomsvurderingerMedId.isEmpty()) {
            log.info("Fant ikke sykdomsvurderinger for behandling $behandlingId, hopper over")
            return
        }

        val vilkårsresultat = try {
            vilkårsresultatRepository.hent(behandlingId)
        } catch (e: Exception) {
            log.info("Behandling $behandlingId har ikke vilkårsresultat, hopper over")
            throw e
        }

        val rettighetsperiode = vilkårsresultat.optionalVilkår(Vilkårtype.ALDERSVILKÅRET)
            ?.tidslinje()
            ?.helePerioden()

        if (rettighetsperiode == null) {
            log.info("Behandling $behandlingId har ikke aldersvilkår/rettighetsperiode, hopper over")
            return
        }

        val sykepengerErstatningGrunnlag = sykepengerErstatningRepository.hentHvisEksisterer(behandlingId)
        val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandlingId)

        for (vurderingMedId in sykdomsvurderingerMedId) {
            val vurdering = vurderingMedId.sykdomsvurdering
            val utledetHalvparten = vurdering.utledErNedsettelseMinstHalvparten()
            val utledetYrkesskade = vurdering.utledErNedsettelseMerEnnYrkesskadegrense()

            sykdomRepository.oppdaterNyeFelter(
                sykdomVurderingId = vurderingMedId.id,
                erNedsettelseMinstHalvparten = utledetHalvparten,
                erNedsettelseMerEnnYrkesskadegrense = utledetYrkesskade
            )
        }

        // Hent oppdatert grunnlag og sammenlign etter oppdatering
        val oppdatertSykdomsGrunnlag = sykdomRepository.hent(behandlingId)
        val oppdatertFaktagrunnlag = SykdomsFaktagrunnlag(
            kravDato = rettighetsperiode.fom,
            sisteDagMedMuligYtelse = rettighetsperiode.tom,
            yrkesskadevurdering = oppdatertSykdomsGrunnlag.yrkesskadevurdering,
            sykepengerErstatningFaktagrunnlag = sykepengerErstatningGrunnlag,
            sykdomsvurderinger = oppdatertSykdomsGrunnlag.sykdomsvurderinger.filter { rettighetsperiode.inneholder(it.vurderingenGjelderFra) },
            bistandvurderingFaktagrunnlag = bistandGrunnlag,
            sykepengeerstatningVilkår = vilkårsresultat.optionalVilkår(Vilkårtype.SYKEPENGEERSTATNING)?.tidslinje()
                .orEmpty(),
        )

        val sammenligningEtter = SykdomsvilkårUtenVissVarighet(vilkårsresultat)
            .vurderOgSammenlign(oppdatertFaktagrunnlag, vilkårsresultat, rettighetsperiode)
        val diffEtter = sammenligningEtter.diff()

        if (diffEtter.isNotEmpty()) {
            val bistandsTidslinje = bistandGrunnlag?.somBistandsvurderingstidslinje().orEmpty()
            val diffEtterFiltrertGamleVurderinger = diffEtter.mapNotNull { diff ->
                if (erSykMenTrengerIkkeBistandOgUtledetMedUtdatertVilkårsvurderingslogikk(bistandsTidslinje, diff)) {
                    log.info("Behandling $behandlingId var oppfylt, men skulle ikke vært det ettersom bistand ikke er oppfylt - ignorerer derfor diff her")
                    null
                } else if (harIkkeFastsattSykdomsvilkåretIOpprinneligBehandling(diff)) {
                    log.info("Behandlingen $behandlingId har ikke kjørt fastsettsykdomsvilkårsteget og er derfor ulikt før og etter migrering")
                    null
                } else if(erYrkesskadeÅrsakssamennhengMenIngentingFør(diff)) {
                    log.info("Var oppfylt med ren bistand tidligere, men viser seg å være yrkesskade årsakssammenheng - ignorerer diff")
                    null
                } else {
                    diff
                }
            }

            if (diffEtterFiltrertGamleVurderinger.isNotEmpty()) {
                if (Miljø.erDev()) {
                    log.warn("Behandlingen $behandlingId har diff etter migrering - ignoreres pga dev-miljø eller whitelisting. Diff: $diffEtter")
                } else if (WHITELISTEDE_BEHANDLING_ID_ER_PRODUKSJON.contains(behandlingId.id)) {
                    log.warn("Behandlingen $behandlingId har diff etter migrering - ignoreres pga whitelisting. Diff: $diffEtter")
                } else {
                    log.error(
                        "Behandling $behandlingId har diff etter migrer. Dette indikerer feil i migreringslogikk. " +
                                "Diff: $diffEtter. Ruller tilbake transaksjon."
                    )


                    throw SykdomsvurderingMigreringFeil(
                        "Migrering av behandling $behandlingId førte til endret vilkårsvurdering. Diff: $diffEtter"
                    )
                }
            } else {
                log.info("Behandling $behandlingId hadde diff etter migrering, men denne var basert på utdatert vilkårvurderingsform på sykdomsvilkåret.")
            }
        }
    }

    private fun harIkkeFastsattSykdomsvilkåretIOpprinneligBehandling(
        diff: Segment<SammenlignetSegment>
    ): Boolean {
        return diff.verdi.gammel?.utfall == Utfall.IKKE_VURDERT
    }

    private fun erYrkesskadeÅrsakssamennhengMenIngentingFør(
        diff: Segment<SammenlignetSegment>
    ): Boolean {
        return diff.verdi.gammel?.utfall == Utfall.OPPFYLT && diff.verdi.ny?.utfall == Utfall.OPPFYLT && diff.verdi.ny?.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG && diff.verdi.gammel?.innvilgelsesårsak == null
    }

    private fun erSykMenTrengerIkkeBistandOgUtledetMedUtdatertVilkårsvurderingslogikk(
        bistandsTidslinje: Tidslinje<Bistandsvurdering>,
        diff: Segment<SammenlignetSegment>
    ): Boolean {
        val bistandIPerioden = bistandsTidslinje.begrensetTil(diff.periode).segmenter()
        val gammelVerdi = diff.verdi.gammel
        val nyVerdi = diff.verdi.ny
        val erNyIkkeOppfylt = nyVerdi?.utfall == Utfall.IKKE_OPPFYLT
        val erGammelOrdinærtOppfylt =
            gammelVerdi?.utfall == Utfall.OPPFYLT && gammelVerdi.innvilgelsesårsak == null
        val harIkkeBistand = bistandIPerioden.any { !it.verdi.erBehovForBistand() }
        val haroppfyltSykdomsvurderingMenIkkeBistandOgBruktUtdatertVilkårsvurderingslogikk =
            erGammelOrdinærtOppfylt && erNyIkkeOppfylt && harIkkeBistand
        return haroppfyltSykdomsvurderingMenIkkeBistandOgBruktUtdatertVilkårsvurderingslogikk
    }
}

class SykdomsvurderingMigreringFeil(message: String) : RuntimeException(message)

