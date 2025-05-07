package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period

class AvklaringsbehovOrkestrator(
    private val connection: DBConnection, private val behandlingHendelseService: BehandlingHendelseService
) {
    // TODO flytt?
    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
    private val prosesserBehandling = ProsesserBehandlingService(flytJobbRepository)

    private val log = LoggerFactory.getLogger(javaClass)

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: AvklaringsbehovLøsning,
        ingenEndringIGruppe: Boolean,
        bruker: Bruker
    ) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        løsAvklaringsbehov(
            kontekst, avklaringsbehovene, avklaringsbehov, bruker, behandling
        )
        markerAvklaringsbehovISammeGruppeForLøst(
            kontekst, ingenEndringIGruppe, avklaringsbehovene, bruker
        )

        fortsettProsessering(kontekst)
    }

    private fun fortsettProsessering(kontekst: FlytKontekst) {
        prosesserBehandling.triggProsesserBehandling(kontekst.sakId, kontekst.behandlingId)
    }

    private fun markerAvklaringsbehovISammeGruppeForLøst(
        kontekst: FlytKontekst, ingenEndringIGruppe: Boolean, avklaringsbehovene: Avklaringsbehovene, bruker: Bruker
    ) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        if (ingenEndringIGruppe && avklaringsbehovene.harVærtSendtTilbakeFraBeslutterTidligere()) {
            val typeBehandling = behandling.typeBehandling()
            val flyt = utledType(typeBehandling).flyt()

            flyt.forberedFlyt(behandling.aktivtSteg())
            val gjenståendeStegIGruppe = flyt.gjenståendeStegIAktivGruppe()

            val behovSomSkalSettesTilAvsluttet = avklaringsbehovene.alle()
                .filter { behov -> gjenståendeStegIGruppe.any { stegType -> behov.løsesISteg() == stegType } }
            log.info("Markerer påfølgende avklaringsbehov[${behovSomSkalSettesTilAvsluttet}] på behandling[${behandling.referanse}] som avsluttet")

            behovSomSkalSettesTilAvsluttet.forEach { avklaringsbehovene.ingenEndring(it, bruker.ident) }
        }
    }

    private fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene,
        avklaringsbehov: AvklaringsbehovLøsning,
        bruker: Bruker,
        behandling: Behandling
    ) {
        val definisjoner = avklaringsbehov.definisjon()
        log.info("Forsøker løse avklaringsbehov[${definisjoner}] på behandling[${behandling.referanse}]")

        avklaringsbehovene.validateTilstand(
            behandling = behandling, avklaringsbehov = definisjoner
        )

        // løses det behov som fremtvinger tilbakehopp?
        val flytOrkestrator = FlytOrkestrator(
            stegKonstruktør = StegKonstruktørImpl(connection),
            ventebehovEvaluererService = VentebehovEvaluererServiceImpl(connection),
            behandlingRepository = behandlingRepository,
            avklaringsbehovRepository = avklaringsbehovRepository,
            informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                repositoryProvider.provide(),
                repositoryProvider,
            ),
            sakRepository = sakRepository,
            perioderTilVurderingService = PerioderTilVurderingService(repositoryProvider),
            sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
            behandlingHendelseService = behandlingHendelseService
        )
        flytOrkestrator.forberedLøsingAvBehov(definisjoner, behandling, kontekst, bruker)

        // Bør ideelt kalle på
        løsFaktiskAvklaringsbehov(kontekst, avklaringsbehovene, avklaringsbehov, bruker)
        log.info("Løste avklaringsbehov[${definisjoner}] på behandling[${behandling.referanse}]")
    }

    private fun løsFaktiskAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene,
        avklaringsbehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker
    ) {
        val løsningsResultat = avklaringsbehovLøsning.løs(connection, AvklaringsbehovKontekst(bruker, kontekst))

        avklaringsbehovene.løsAvklaringsbehov(
            avklaringsbehovLøsning.definisjon(),
            løsningsResultat.begrunnelse,
            bruker.ident,
            løsningsResultat.kreverToTrinn
        )
    }

    fun settBehandlingPåVent(behandlingId: BehandlingId, hendelse: BehandlingSattPåVent) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.MANUELT_SATT_PÅ_VENT),
            funnetISteg = behandling.aktivtSteg(),
            frist = hendelse.frist,
            begrunnelse = hendelse.begrunnelse,
            grunn = hendelse.grunn,
            bruker = hendelse.bruker
        )

        avklaringsbehovene.validateTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)
        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }

    fun settPåVentMensVentePåMedisinskeOpplysninger(behandlingId: BehandlingId, bruker: Bruker) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.BESTILL_LEGEERKLÆRING),
            funnetISteg = behandling.aktivtSteg(),
            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
            bruker = bruker,
            frist = LocalDate.now() + Period.ofWeeks(4),
        )
        avklaringsbehovene.validateTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }

    fun opprettSkrivBrevAvklaringsbehov(behandlingId: BehandlingId, typeBrev: TypeBrev) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = behandlingId)
        val definisjon = if (typeBrev.erVedtak() && Miljø.er() == MiljøKode.DEV) {
            Definisjon.SKRIV_VEDTAKSBREV
        } else {
            Definisjon.SKRIV_BREV
        }
        avklaringsbehovene.leggTil(listOf(definisjon), behandling.aktivtSteg())
        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }
}
