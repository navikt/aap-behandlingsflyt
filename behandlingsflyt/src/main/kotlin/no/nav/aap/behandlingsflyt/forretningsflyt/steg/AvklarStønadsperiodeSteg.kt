package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.erSystembruker
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.utils.tilNorskFormat
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant
import java.time.LocalDate

class AvklarStønadsperiodeSteg(
    private val unleashGateway: UnleashGateway,
    private val kravRepository: KravRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val arenaMaksdatoRepository: ArenaMaksdatoRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                if(kontekst.erMigreringFraArena()) {
                    vurderMigrertKravAutomatisk(kontekst)
                } else {
                    vurderAutomatisk(kontekst)
                }
            }

            else -> {}
        }

        return Fullført
    }

    private fun vurderAutomatisk(kontekst: FlytKontekstMedPerioder) {
        val gjeldendeRelevanteKrav =
            kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeRelevanteKrav().orEmpty()
        val vedtatteStønadsperiodeGrunnlag = kontekst.forrigeBehandlingId?.let {
            stønadsperiodeRepository.hentHvisEksisterer(it)
        }

        val arenaMaksdato = hentArenaMaksdato(kontekst)

        val kravSomManglerVurdering =
            gjeldendeRelevanteKrav.filter { krav ->
                val vedtatteStønadsperiodeForVurdering = vedtatteStønadsperiodeGrunnlag?.gjeldendeVurderinger()
                    ?.firstOrNull { it.referanse == krav.referanse }
                val forventetStartdato = forventetStartdato(krav, gjeldendeRelevanteKrav, arenaMaksdato)

                vedtatteStønadsperiodeForVurdering == null || (
                        vedtatteStønadsperiodeForVurdering.vurdertAv.erSystembruker() &&
                                vedtatteStønadsperiodeForVurdering.startDato != forventetStartdato
                        )
            }

        val nyeVurderinger = kravSomManglerVurdering.map {
            vurderStønadsperiode(it, gjeldendeRelevanteKrav, arenaMaksdato, kontekst)
        }

        stønadsperiodeRepository.lagre(kontekst.behandlingId, vedtatteStønadsperiodeGrunnlag?.vurderinger.orEmpty() + nyeVurderinger)
    }

    private fun hentArenaMaksdato(kontekst: FlytKontekstMedPerioder): ArenaMaksdato? {
        if (!unleashGateway.isEnabled(BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)) return null
        return arenaMaksdatoRepository.hentHvisEksisterer(kontekst.behandlingId)?.arenaMaksdato
    }

    private fun vurderMigrertKravAutomatisk(kontekst: FlytKontekstMedPerioder) {
        kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeMigrertKrav()?.let { migrertKrav ->
            stønadsperiodeRepository.lagre(kontekst.behandlingId, setOf(StønadsperiodeVurdering(
                referanse = migrertKrav.referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = kontekst.behandlingId,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Migrert stønadsperiode fra Arenasak ${migrertKrav.arenaSaksnummer}",
                harHattOrdinærSiste52Uker = false,
                harGjenværendeKvote = false,
                relevantKravType = RelevantKravType.MIGRERT_STØNADSPERIODE,
                startDato = migrertKrav.muligRettFra
            )))
        }
    }

    private fun vurderStønadsperiode(
        krav: RelevantKrav,
        gjeldendeRelevanteKrav: Set<RelevantKrav>,
        arenaMaksdato: ArenaMaksdato?,
        kontekst: FlytKontekstMedPerioder,
    ): StønadsperiodeVurdering {
        if (arenaMaksdato?.maksdato != null && erKantIKantMedArena(krav, gjeldendeRelevanteKrav, arenaMaksdato)) {
            return StønadsperiodeVurdering(
                referanse = krav.referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = kontekst.behandlingId,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Tilstøtende avsluttet Arena-sak ${arenaMaksdato.arenaSaksnummer}, " +
                        "stønadsperiode starter dagen etter maksdato ${arenaMaksdato.maksdato.tilNorskFormat()}",
                harHattOrdinærSiste52Uker = true,
                harGjenværendeKvote = false,
                relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                startDato = arenaMaksdato.maksdato.plusDays(1),
            )
        }

        // TODO: Antar ny stønadsperiode enn så lenge
        return StønadsperiodeVurdering(
            referanse = krav.referanse,
            opprettet = Instant.now(),
            vurdertIBehandling = kontekst.behandlingId,
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Automatisk vurdering",
            harHattOrdinærSiste52Uker = false,
            harGjenværendeKvote = false,
            relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
            startDato = krav.muligRettFra // Dum heuristikk på at stønadsperiode start = kravdato start
        )
    }

    private fun forventetStartdato(
        krav: RelevantKrav,
        gjeldendeRelevanteKrav: Set<RelevantKrav>,
        arenaMaksdato: ArenaMaksdato?,
    ): LocalDate {
        val maksdato = arenaMaksdato?.maksdato
        return if (maksdato != null && erKantIKantMedArena(krav, gjeldendeRelevanteKrav, arenaMaksdato)) {
            maksdato.plusDays(1)
        } else {
            krav.muligRettFra
        }
    }

    /**
     * Første krav i saken er kant i kant med Arena når det har mulig rett fra senest dagen etter Arena-maksdato,
     * og dagen etter maksdato ligger innenfor kravets periode.
     */
    private fun erKantIKantMedArena(
        krav: RelevantKrav,
        gjeldendeRelevanteKrav: Set<RelevantKrav>,
        arenaMaksdato: ArenaMaksdato?,
    ): Boolean {
        val maksdato = arenaMaksdato?.maksdato ?: return false
        val kravSortert = gjeldendeRelevanteKrav.sortedBy { it.muligRettFra }
        if (kravSortert.first().referanse != krav.referanse) return false

        val dagenEtterMaksdato = maksdato.plusDays(1)
        if (krav.muligRettFra.isAfter(dagenEtterMaksdato)) return false

        val nesteKravStart = kravSortert.getOrNull(1)?.muligRettFra
        return nesteKravStart == null || dagenEtterMaksdato.isBefore(nesteKravStart)
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvklarStønadsperiodeSteg(
                unleashGateway = gatewayProvider.provide(),
                kravRepository = repositoryProvider.provide(),
                stønadsperiodeRepository = repositoryProvider.provide(),
                arenaMaksdatoRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_STØNADSPERIODE
        }
    }

}