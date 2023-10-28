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
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.prosessering.Gruppe
import no.nav.aap.behandlingsflyt.prosessering.OppgaveInput
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgave
import org.slf4j.LoggerFactory

class AvklaringsbehovOrkestrator(private val connection: DBConnection) {

    private val avklaringsbehovsLøsere = mutableMapOf<Definisjon, AvklaringsbehovsLøser<*>>()

    private val log = LoggerFactory.getLogger(AvklaringsbehovOrkestrator::class.java)

    init {
        avklaringsbehovsLøsere[Definisjon.MANUELT_SATT_PÅ_VENT] = SattPåVentLøser()
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKDOM] = AvklarSykdomLøser()
        avklaringsbehovsLøsere[Definisjon.FRITAK_MELDEPLIKT] = FritakFraMeldepliktLøser()
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKEPENGEERSTATNING] = AvklarSykepengerErstatningLøser()
        avklaringsbehovsLøsere[Definisjon.AVKLAR_BISTANDSBEHOV] = AvklarBistandLøser()
        avklaringsbehovsLøsere[Definisjon.AVKLAR_YRKESSKADE] = AvklarYrkesskadeLøser()
        avklaringsbehovsLøsere[Definisjon.FORESLÅ_VEDTAK] = ForeslåVedtakLøser()
        avklaringsbehovsLøsere[Definisjon.AVKLAR_STUDENT] = AvklarStudentLøser()
        avklaringsbehovsLøsere[Definisjon.FATTE_VEDTAK] = FatteVedtakLøser()
    }

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: List<AvklaringsbehovLøsning>
    ) {
        løsAvklaringsbehov(kontekst, avklaringsbehov)

        OppgaveRepository.leggTil(
            Gruppe().leggTil(
                OppgaveInput(oppgave = ProsesserBehandlingOppgave).forBehandling(
                    kontekst.sakId,
                    kontekst.behandlingId
                )
            )
        )
    }

    fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehov: List<AvklaringsbehovLøsning>
    ) {
        val behandling = BehandlingRepository.hent(kontekst.behandlingId)
        log.info("Forsøker løse avklaringsbehov på behandling[${behandling.referanse}")

        val definisjoner = avklaringsbehov.map { løsning -> løsning.definisjon() }

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
    }
}
