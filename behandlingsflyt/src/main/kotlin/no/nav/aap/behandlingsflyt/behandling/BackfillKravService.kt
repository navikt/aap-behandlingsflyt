package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class BackfillKravService(
    private val kravRepository: KravRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val trukketSøknadService: TrukketSøknadService,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        kravRepository = repositoryProvider.provide(),
        stønadsperiodeRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
    )

    fun erTrukketSøknadSak(behandlinger: List<Behandling>): Boolean =
        behandlinger.filter { it.status().erAvsluttet() }.any { trukketSøknadService.søknadErTrukket(it.id) }

    /**
     * Backfiller krav og stønadsperiode for én behandling.
     *
     * Returnerer [BackfillBehandlingResultat.NullKrav] dersom behandlingen allerede hadde krav –
     * løkken i runner skal da bryte ut av saken, da resten er nyere og allerede har sine vurderinger.
     */
    fun backfillBehandling(behandling: Behandling): BackfillBehandlingResultat {
        val eksisterendeKrav =
            kravRepository.hentHvisEksisterer(behandling.id) ?: return BackfillBehandlingResultat.NullKrav
        backfillStønadsperiode(behandling.id, eksisterendeKrav, behandling.forrigeBehandlingId)

        return BackfillBehandlingResultat.Backfilled
    }

    /**
     * Vurderinger fra forrige behandling videreføres (kopieres) – kun krav som mangler vurdering,
     * eller har en automatisk (SYSTEMBRUKER) vurdering der startDato ikke lenger stemmer med
     * krav.muligRettFra, får en ny automatisk vurdering. Dette speiler logikken i
     * [no.nav.aap.behandlingsflyt.forretningsflyt.steg.AvklarStønadsperiodeSteg].
     *
     * Vurderinger for krav som ikke lenger er blant gjeldende relevante krav (f.eks. et krav som er
     * nedgradert til tilleggsopplysning) skal ikke fjernes – de filtreres bort ved bruk
     */
    private fun backfillStønadsperiode(
        behandlingId: BehandlingId,
        grunnlag: KravGrunnlag,
        forrigeBehandlingId: BehandlingId?,
    ) {
        val gjeldendeRelevanteKrav = grunnlag.gjeldendeRelevanteKrav()
        if (gjeldendeRelevanteKrav.isEmpty()) return

        val referanser = gjeldendeRelevanteKrav.map { it.referanse }
        check(referanser.size <= 1) {
            "Fant flere distinkte kravreferanser blant relevante krav for behandling $behandlingId – forventet maks én"
        }

        val vedtatteStønadsperiodeVurderinger = forrigeBehandlingId
            ?.let { stønadsperiodeRepository.hentHvisEksisterer(it)?.gjeldendeVurderinger() }
            .orEmpty()

        val kravSomManglerVurdering = gjeldendeRelevanteKrav.filter { krav ->
            val vedtattStønadsperiodeForKrav =
                vedtatteStønadsperiodeVurderinger.firstOrNull { it.referanse == krav.referanse }
            vedtattStønadsperiodeForKrav == null || (
                    vedtattStønadsperiodeForKrav.vurdertAv == SYSTEMBRUKER &&
                            vedtattStønadsperiodeForKrav.startDato != krav.muligRettFra
                    )
        }

        val nyeVurderinger = kravSomManglerVurdering.map { krav ->
            StønadsperiodeVurdering(
                referanse = krav.referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = behandlingId,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Automatisk vurdert",
                harHattOrdinærSiste52Uker = false,
                harGjenværendeKvote = false,
                relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                startDato = krav.muligRettFra,
            )
        }

        stønadsperiodeRepository.lagre(behandlingId, vedtatteStønadsperiodeVurderinger + nyeVurderinger)
    }
}

enum class BackfillBehandlingResultat {
    NullKrav,

    /** Krav ble backfilled (eller det var ingenting å gjøre). */
    Backfilled,
}
