package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.SattPåVentLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.AvklarBistandLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.FritakFraMeldepliktLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.student.AvklarStudentLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarSykdomLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarSykepengerErstatningLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarYrkesskadeLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.FatteVedtakLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.ForeslåVedtakLøser
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.prosessering.OppgaveInput
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgave
import org.slf4j.LoggerFactory

class AvklaringsbehovOrkestrator(private val connection: DBConnection) {

    private val avklaringsbehovsLøsere = mutableMapOf<Definisjon, AvklaringsbehovsLøser<*>>()
    private val avklaringsbehovRepository = AvklaringsbehovRepository(connection)

    private val log = LoggerFactory.getLogger(AvklaringsbehovOrkestrator::class.java)

    init {
        avklaringsbehovsLøsere[Definisjon.MANUELT_SATT_PÅ_VENT] = SattPåVentLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKDOM] = AvklarSykdomLøser(connection)
        avklaringsbehovsLøsere[Definisjon.FRITAK_MELDEPLIKT] = FritakFraMeldepliktLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKEPENGEERSTATNING] = AvklarSykepengerErstatningLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_BISTANDSBEHOV] = AvklarBistandLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_YRKESSKADE] = AvklarYrkesskadeLøser(connection)
        avklaringsbehovsLøsere[Definisjon.FORESLÅ_VEDTAK] = ForeslåVedtakLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_STUDENT] = AvklarStudentLøser(connection)
        avklaringsbehovsLøsere[Definisjon.FATTE_VEDTAK] = FatteVedtakLøser(connection)
    }

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: List<AvklaringsbehovLøsning>
    ) {
        løsAvklaringsbehov(kontekst, avklaringsbehov)

        OppgaveRepository(connection).leggTil(
            OppgaveInput(oppgave = ProsesserBehandlingOppgave).forBehandling(
                kontekst.sakId,
                kontekst.behandlingId
            )
        )
    }

    fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehov: List<AvklaringsbehovLøsning>
    ) {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)
        val definisjoner = avklaringsbehov.map { løsning -> løsning.definisjon() }
        log.info("Forsøker løse avklaringsbehov[${definisjoner}] på behandling[${behandling.referanse}]")

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, definisjoner)

        // løses det behov som fremtvinger tilbakehopp?
        val flytOrkestrator = FlytOrkestrator(connection)
        flytOrkestrator.forberedLøsingAvBehov(definisjoner, behandling, kontekst)

        // Bør ideelt kalle på
        avklaringsbehov.forEach { løsAvklaringsbehov(kontekst, behandling, it) }
    }


    @Suppress("UNCHECKED_CAST")
    private fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        behandling: Behandling,
        it: AvklaringsbehovLøsning
    ) {
        // Liker denne casten fryktelig lite godt -_- men må til pga generics *
        val avklaringsbehovsLøser =
            avklaringsbehovsLøsere.getValue(it.definisjon()) as AvklaringsbehovsLøser<AvklaringsbehovLøsning>
        val løsningsResultat = avklaringsbehovsLøser.løs(kontekst = kontekst, løsning = it)
        val avklaringsbehovene = behandling.avklaringsbehovene()
        if (løsningsResultat.kreverToTrinn == null) {
            avklaringsbehovene.løsAvklaringsbehov(
                it.definisjon(),
                løsningsResultat.begrunnelse,
                "Saksbehandler" // TODO: Hente fra context
            )
        } else {
            avklaringsbehovene.løsAvklaringsbehov(
                it.definisjon(),
                løsningsResultat.begrunnelse,
                "Saksbehandler", // TODO: Hente fra context
                løsningsResultat.kreverToTrinn
            )
        }
        avklaringsbehovRepository.løs(
            behandlingId = behandling.id,
            definisjon = it.definisjon(),
            begrunnelse = løsningsResultat.begrunnelse,
            kreverToTrinn = løsningsResultat.kreverToTrinn
        )
    }
}
