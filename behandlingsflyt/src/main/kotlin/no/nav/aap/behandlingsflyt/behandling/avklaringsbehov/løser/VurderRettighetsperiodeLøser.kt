package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.DatoFraDokumentUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravValidering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeHarRett
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class VurderRettighetsperiodeLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val rettighetsperiodeRepository: VurderRettighetsperiodeRepository,
    private val sakService: SakService,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val kravRepository: KravRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<VurderRettighetsperiodeLøsning> {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        rettighetsperiodeRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider, gatewayProvider),
        mottattDokumentRepository = repositoryProvider.provide(),
        kravRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderRettighetsperiodeLøsning): LøsningsResultat {

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        if (behandling.status().erAvsluttet()) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at behandlingen er avsluttet")
        }
        if (sak.status() == Status.AVSLUTTET) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at saken er avsluttet")
        }
        val nyStartDato = løsning.rettighetsperiodeVurdering.startDato
        if (innskrenkerTilEtterSøknadstidspunkt(nyStartDato, sak.id)) {
            throw UgyldigForespørselException("Kan ikke endre starttidspunkt til å gjelde ETTER søknadstidspunkt")
        }

        val vurdering = RettighetsperiodeVurdering(
            begrunnelse = løsning.rettighetsperiodeVurdering.begrunnelse,
            startDato = nyStartDato,
            harRettUtoverSøknadsdato = løsning.rettighetsperiodeVurdering.harRett,
            vurdertAv = kontekst.bruker.ident,
            vurdertDato = LocalDateTime.now()
        )

        rettighetsperiodeRepository.lagreVurdering(
            behandlingId = behandling.id,
            vurdering = vurdering
        )

        if (løsning.rettighetsperiodeVurdering.harRett.harOverstyrt() && nyStartDato != null) {
            log.info("Oppdaterer rettighetsperioden til å gjelde fra $ for sak ${sak.id}")
            sakService.overstyrRettighetsperioden(
                sakId = sak.id,
                startDato = nyStartDato,
                sluttDato = Tid.MAKS
            )
            if (unleashGateway.isEnabled(BehandlingsflytFeature.LagreVurderRettighetsperiodeSomKrav)) {
                oppdaterKravForOverstyrtMuligRett(kontekst.sakId(), kontekst.behandlingId(), vurdering)
            }
        } else if (!løsning.rettighetsperiodeVurdering.harRett.harOverstyrt()) {
            val søknadsdato = finnSøknadsdatoForSak(sak.id)
                ?: throw UgyldigForespørselException("Forsøker å tilbakestille rettighetsperioden, men finner ingen søknadsdato for saken")
            if (sak.rettighetsperiode.fom != søknadsdato) {
                log.info("Tilbakestiller rettighetsperioden til å gjelde fra søknadsdato $søknadsdato for sak ${sak.id}")
                sakService.overstyrRettighetsperioden(
                    sakId = sak.id,
                    startDato = søknadsdato,
                    sluttDato = Tid.MAKS
                )
                if (unleashGateway.isEnabled(BehandlingsflytFeature.LagreVurderRettighetsperiodeSomKrav)) {
                    reverserKravForOverstyrtMuligRett(kontekst.behandlingId(), vurdering)
                }
            }
        }

        return LøsningsResultat("Vurdert rettighetsperiode")
    }

    private fun innskrenkerTilEtterSøknadstidspunkt(startDato: LocalDate?, sakId: SakId): Boolean {
        val søknadsdato = finnSøknadsdatoForSak(sakId)
        return startDato != null && søknadsdato != null && startDato.isAfter(søknadsdato)
    }

    private fun finnSøknadsdatoForSak(sakId: SakId): LocalDate? =
        DatoFraDokumentUtleder(mottattDokumentRepository).utledSøknadsdatoForSak(sakId)?.toLocalDate()

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_RETTIGHETSPERIODE
    }

    private fun oppdaterKravForOverstyrtMuligRett(
        sakId: SakId,
        behandlingId: BehandlingId,
        rettighetsperiodeVurdering: RettighetsperiodeVurdering
    ) {
        if (rettighetsperiodeVurdering.startDato == null || !rettighetsperiodeVurdering.harRettUtoverSøknadsdato.kanUtledeOverstyrMuligRettFraÅrsak()) {
            throw IllegalStateException("Klarte ikke å utlede kravvurdering for løsning")
        }

        val eksisterendeKravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId)

        // Antar at det første kravet med type NyttKrav er det migrerte kravet
        val førsteKrav =
            eksisterendeKravGrunnlag?.vurderinger
                ?.filterIsInstance<NyttKrav>()?.minBy { it.opprettet }

        if (førsteKrav == null) {
            log.info("Fant ikke NyttKrav for sak ${sakId}, lagrer ikke ned kravvurdering for rettighetsperiodeløsning")
            return
        }

        val søknadForKrav = mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.SØKNAD)
            .find { it.referanse == førsteKrav.journalpostId }

        val nyVurdering = NyttKrav(
            referanse = førsteKrav.referanse,
            journalpostId = førsteKrav.journalpostId,
            vurdertAv = Bruker(rettighetsperiodeVurdering.vurdertAv),
            begrunnelse = rettighetsperiodeVurdering.begrunnelse,
            vurdertIBehandling = behandlingId,
            opprettet = rettighetsperiodeVurdering.vurdertDato.toInstant(ZoneOffset.UTC),
            søknadsdato = førsteKrav.søknadsdato,
            overstyrMuligRettFra = OverstyrMuligRettFra(
                dato = rettighetsperiodeVurdering.startDato,
                årsak = rettighetsperiodeVurdering.harRettUtoverSøknadsdato.tilOverstyrMuligRettFraÅrsak()
            ),
            muligRettFra = rettighetsperiodeVurdering.startDato,
        )

        KravValidering.validerKravMedDato(nyVurdering, søknadForKrav)

        kravRepository.lagre(
            behandlingId = behandlingId,
            vurderinger =
                eksisterendeKravGrunnlag.vurderinger
                    .filterNot { it.vurdertIBehandling == behandlingId && it.erRettighetsperiodeVurdering() }
                    .toSet() + nyVurdering
        )
    }

    private fun reverserKravForOverstyrtMuligRett(
        behandlingId: BehandlingId,
        rettighetsperiodeVurdering: RettighetsperiodeVurdering
    ) {
        if (rettighetsperiodeVurdering.harRettUtoverSøknadsdato != RettighetsperiodeHarRett.Nei) {
            throw IllegalStateException("Forventer at vurdering som skal reverseres har RettighetsperiodeHarRett.Nei")
        }

        val eksisterendeKravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId)

        /** Så lenge vi skrur av rettighetsperiodesteget før vi skrur på manuell løsning av krav, 
         * kan vi anta at alle overstyrte krav i inneværende behandling kan fjernes fra grunnlaget
         * Det kan ha kommet andre automatiske kravurderinger i mellomtiden */
        if (eksisterendeKravGrunnlag != null) {
            kravRepository.lagre(
                behandlingId = behandlingId,
                vurderinger = eksisterendeKravGrunnlag.vurderinger
                    .filterNot { it.vurdertIBehandling == behandlingId && it.erRettighetsperiodeVurdering() }
                    .toSet()
            )
        }
    }


    private fun KravVurdering.erRettighetsperiodeVurdering(): Boolean {
        return this is NyttKrav && this.overstyrMuligRettFra != null
    }
}

