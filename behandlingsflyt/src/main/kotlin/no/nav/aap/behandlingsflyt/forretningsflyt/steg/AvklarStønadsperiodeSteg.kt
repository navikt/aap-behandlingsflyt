package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.erSystembruker
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.kravtidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Instant

class AvklarStønadsperiodeSteg(
    private val unleashGateway: UnleashGateway,
    private val kravRepository: KravRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val arenaMaksdatoRepository: ArenaMaksdatoRepository,
    private val sakService: SakService,
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

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
        val vedtattGrunnlag = kontekst.forrigeBehandlingId?.let(stønadsperiodeRepository::hentHvisEksisterer)
        val vedtatteVurderinger = vedtattGrunnlag?.gjeldendeVurderinger().orEmpty().associateBy { it.referanse }
        val arenaMaksdato = hentArenaMaksdato(kontekst)
        val kantIKantKrav = kravVedOvergangFraArena(gjeldendeRelevanteKrav, arenaMaksdato)
        if (kantIKantKrav != null) {
            sjekkAtIngenKravFørKantIKantKrav(kantIKantKrav, gjeldendeRelevanteKrav)
        }

        val nyeVurderinger = gjeldendeRelevanteKrav
            .map { krav -> vurderStønadsperiode(krav, kantIKantKrav, arenaMaksdato, kontekst) }
            .filter { trengerNyVurdering(vedtatteVurderinger[it.referanse], it) }

        // Svarer på om forrige lagrede vurdering var kant-i-kant. Må hentes før nye vurderinger lagres.
        val forrigeVurderingVarKantIKant = erKantIKant(kontekst.behandlingId)
        stønadsperiodeRepository.lagre(kontekst.behandlingId, vedtattGrunnlag?.vurderinger.orEmpty() + nyeVurderinger)
        oppdaterRettighetsperiodeVedKantIKant(kontekst, forrigeVurderingVarKantIKant, arenaMaksdato)
    }

    private fun erKantIKant(behandlingId: BehandlingId): Boolean =
        stønadsperiodeRepository.hentHvisEksisterer(behandlingId)
            ?.gjeldendeVurderinger().orEmpty()
            .any { it.erKantIKantMedArena }

    /**
     * TODO: Midlertidig til stønadsperioden er fasit i hele flyten. Rydder ikke vilkårsresultatet.
     *
     * Setter fom i rettighetsperioden lik tidligste startdato i stønadsperioden når saken er kant i kant,
     * eller når forrige vurdering i behandlingen var kant i kant (tilbakestilling). Saker som verken er
     * eller var kant i kant røres ikke. Med toggle av skjer verken overstyring eller tilbakestilling.
     */
    private fun oppdaterRettighetsperiodeVedKantIKant(
        kontekst: FlytKontekstMedPerioder,
        forrigeVurderingVarKantIKant: Boolean,
        arenaMaksdato: ArenaMaksdato?,
    ) {
        // Rettighetsperioden ligger på saken. Vedtatte saker skal ikke få den flyttet med tilbakevirkning.
        if (kontekst.forrigeBehandlingId != null) return
        if (!unleashGateway.isEnabled(BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)) return

        val vurderinger = stønadsperiodeRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.gjeldendeVurderinger().orEmpty()
        val erKantIKant = vurderinger.any { it.erKantIKantMedArena }
        if (!erKantIKant && !forrigeVurderingVarKantIKant) return

        val nyFom = vurderinger.minOfOrNull { it.startDato } ?: return
        val sak = sakService.hent(kontekst.sakId)
        val gjeldendeFom = sak.rettighetsperiode.fom
        if (nyFom == gjeldendeFom) return

        log.info(
            "Endrer fom i rettighetsperioden for sak ${sak.saksnummer} fra $gjeldendeFom til $nyFom. " +
                    "Årsak: ${årsakTilEndring(erKantIKant, arenaMaksdato)}"
        )
        sakService.overstyrRettighetsperioden(sak, nyFom, Tid.MAKS)
    }

    private fun årsakTilEndring(erKantIKant: Boolean, arenaMaksdato: ArenaMaksdato?): String =
        if (erKantIKant) "kant i kant med Arena-sak ${arenaMaksdato?.arenaSaksnummer}"
        else "tilbakestilt, ikke lenger kant i kant (Arena-sak ${arenaMaksdato?.arenaSaksnummer})"

    private fun trengerNyVurdering(vedtatt: StønadsperiodeVurdering?, forslag: StønadsperiodeVurdering) =
        vedtatt == null || (vedtatt.vurdertAv.erSystembruker() && vedtatt.startDato != forslag.startDato)

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
        kantIKantKrav: RelevantKrav?,
        arenaMaksdato: ArenaMaksdato?,
        kontekst: FlytKontekstMedPerioder,
    ): StønadsperiodeVurdering {
        if (arenaMaksdato?.maksdato != null && krav.referanse == kantIKantKrav?.referanse) {
            return StønadsperiodeVurdering(
                referanse = krav.referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = kontekst.behandlingId,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Startdatoen er dagen etter siste dag med rett til AAP i tilstøtende " +
                        "Arena-sak ${arenaMaksdato.arenaSaksnummer}",
                harHattOrdinærSiste52Uker = true,
                harGjenværendeKvote = false,
                relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                startDato = arenaMaksdato.maksdato.plusDays(1),
                erKantIKantMedArena = true,
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

    /**
     * Finner kravet som er kant i kant med Arena: kravet hvis segment i kravtidslinjen inneholder
     * dagen etter Arena-maksdato. Kravtidslinjen avgrenser hvert krav ved starten på neste krav.
     * Returnerer null når ingen krav dekker datoen, for eksempel ved opphold etter Arena-saken.
     */
    private fun kravVedOvergangFraArena(
        gjeldendeRelevanteKrav: Set<RelevantKrav>,
        arenaMaksdato: ArenaMaksdato?,
    ): RelevantKrav? {
        val dagenEtterMaksdato = arenaMaksdato?.maksdato?.plusDays(1) ?: return null
        return gjeldendeRelevanteKrav.kravtidslinje()
            .segmenter()
            .singleOrNull { it.periode.inneholder(dagenEtterMaksdato) }
            ?.verdi
    }

    /**
     * Midlertidig sperre. Det er ikke avklart hvordan tidligere krav skal håndteres når et senere krav
     * er kant i kant med Arena, og flyten støtter ikke manuell vurdering av stønadsperiode ennå.
     * Vi stopper derfor heller enn å lagre en automatisk vurdering vi ikke er trygge på.
     */
    private fun sjekkAtIngenKravFørKantIKantKrav(
        kantIKantKrav: RelevantKrav,
        gjeldendeRelevanteKrav: Set<RelevantKrav>,
    ) {
        val harTidligereKrav = gjeldendeRelevanteKrav.any { it.muligRettFra.isBefore(kantIKantKrav.muligRettFra) }
        check(!harTidligereKrav) {
            "Støtter ikke kant i kant med Arena når saken har relevante krav før kravet " +
                    "${kantIKantKrav.referanse.verdi}, som dekker dagen etter maksdato."
        }
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
                sakService = SakService(repositoryProvider, gatewayProvider),
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_STØNADSPERIODE
        }
    }

}
