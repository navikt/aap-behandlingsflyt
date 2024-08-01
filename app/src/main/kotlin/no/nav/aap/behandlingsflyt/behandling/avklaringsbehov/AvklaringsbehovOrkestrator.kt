package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.auth.Bruker
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SattPåVentLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.slf4j.LoggerFactory

class AvklaringsbehovOrkestrator(
    private val connection: DBConnection, private val behandlingHendelseService: BehandlingHendelseService
) {

    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val flytJobbRepository = FlytJobbRepository(connection)

    private val log = LoggerFactory.getLogger(AvklaringsbehovOrkestrator::class.java)

    fun taAvVentHvisPåVentOgFortsettProsessering(behandlingId: BehandlingId) {
        val behandling = behandlingRepository.hent(behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        val kontekst = behandling.flytKontekst()
        if (avklaringsbehovene.erSattPåVent()) {
            løsAvklaringsbehov(
                kontekst = kontekst,
                avklaringsbehovene = avklaringsbehovene,
                avklaringsbehov = SattPåVentLøsning(),
                bruker = SYSTEMBRUKER,
                behandling = behandling
            )
        }
        fortsettProsessering(kontekst)
    }

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
        flytJobbRepository.leggTil(
            JobbInput(jobb = ProsesserBehandlingJobbUtfører).forBehandling(
                kontekst.sakId, kontekst.behandlingId
            ).medCallId()
        )
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
        val flytOrkestrator = FlytOrkestrator(connection)
        flytOrkestrator.forberedLøsingAvBehov(definisjoner, behandling, kontekst)

        // Bør ideelt kalle på
        løsFaktiskAvklaringsbehov(kontekst, avklaringsbehovene, avklaringsbehov, bruker)
    }

    private fun løsFaktiskAvklaringsbehov(
        kontekst: FlytKontekst, avklaringsbehovene: Avklaringsbehovene, it: AvklaringsbehovLøsning, bruker: Bruker
    ) {
        avklaringsbehovene.leggTilFrivilligHvisMangler(it.definisjon(), bruker)
        val løsningsResultat = it.løs(connection, AvklaringsbehovKontekst(bruker, kontekst))

        avklaringsbehovene.løsAvklaringsbehov(
            it.definisjon(), løsningsResultat.begrunnelse, bruker.ident, løsningsResultat.kreverToTrinn
        )
    }

    fun settBehandlingPåVent(behandlingId: BehandlingId, hendelse: BehandlingSattPåVent) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.MANUELT_SATT_PÅ_VENT),
            stegType = behandling.aktivtSteg(),
            frist = hendelse.frist,
            begrunnelse = hendelse.begrunnelse,
            grunn = hendelse.grunn,
            bruker = hendelse.bruker
        )

        avklaringsbehovene.validateTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)
        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }
}
