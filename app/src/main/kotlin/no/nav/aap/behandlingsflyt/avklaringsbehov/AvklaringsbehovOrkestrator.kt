package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.SattPåVentLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne.FastsettArbeidsevneLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.AvklarBistandLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt.FritakFraMeldepliktLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.AvklarStudentLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykdomLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykepengerErstatningLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarYrkesskadeLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.FatteVedtakLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ForeslåVedtakLøser
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgave
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveRepository
import no.nav.aap.verdityper.flyt.FlytKontekst
import org.slf4j.LoggerFactory

class AvklaringsbehovOrkestrator(private val connection: DBConnection) {

    private val avklaringsbehovsLøsere = mutableMapOf<Definisjon, AvklaringsbehovsLøser<*>>()
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val behandlingRepository = behandlingRepository(connection)
    private val oppgaveRepository = OppgaveRepository(connection)

    private val log = LoggerFactory.getLogger(AvklaringsbehovOrkestrator::class.java)

    init {
        avklaringsbehovsLøsere[Definisjon.MANUELT_SATT_PÅ_VENT] = SattPåVentLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKDOM] = AvklarSykdomLøser(connection)
        avklaringsbehovsLøsere[Definisjon.FRITAK_MELDEPLIKT] = FritakFraMeldepliktLøser(connection)
        avklaringsbehovsLøsere[Definisjon.FASTSETT_ARBEIDSEVNE] = FastsettArbeidsevneLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKEPENGEERSTATNING] = AvklarSykepengerErstatningLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_BISTANDSBEHOV] = AvklarBistandLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_YRKESSKADE] = AvklarYrkesskadeLøser(connection)
        avklaringsbehovsLøsere[Definisjon.FORESLÅ_VEDTAK] = ForeslåVedtakLøser(connection)
        avklaringsbehovsLøsere[Definisjon.AVKLAR_STUDENT] = AvklarStudentLøser(connection)
        avklaringsbehovsLøsere[Definisjon.FATTE_VEDTAK] = FatteVedtakLøser(connection)
    }

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: AvklaringsbehovLøsning,
        ingenEndringIGruppe: Boolean
    ) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        løsAvklaringsbehov(kontekst, avklaringsbehovene, avklaringsbehov)
        markerAvklaringsbehovISammeGruppeForLøst(
            kontekst,
            ingenEndringIGruppe,
            avklaringsbehovene
        )

        oppgaveRepository.leggTil(
            OppgaveInput(oppgave = ProsesserBehandlingOppgave).forBehandling(
                kontekst.sakId,
                kontekst.behandlingId
            )
        )
    }

    private fun markerAvklaringsbehovISammeGruppeForLøst(
        kontekst: FlytKontekst,
        ingenEndringIGruppe: Boolean,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        if (ingenEndringIGruppe && avklaringsbehovene.harVærtSendtTilbakeFraBeslutterTidligere()) {
            val typeBehandling = behandling.typeBehandling()
            val flyt = utledType(typeBehandling).flyt()

            flyt.forberedFlyt(behandling.aktivtSteg())
            val gjenståendeStegIGruppe = flyt.gjenståendeStegIAktivGruppe()

            val behovSomSkalSettesTilAvsluttet =
                avklaringsbehovene.alle()
                    .filter { behov -> gjenståendeStegIGruppe.any { stegType -> behov.løsesISteg() == stegType } }
            log.info("Markerer påfølgende avklaringsbehov[${behovSomSkalSettesTilAvsluttet}] på behandling[${behandling.referanse}] som avsluttet")

            behovSomSkalSettesTilAvsluttet.forEach { avklaringsbehovene.ingenEndring(it) }
        }
    }

    fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene,
        avklaringsbehov: AvklaringsbehovLøsning
    ) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val definisjoner = avklaringsbehov.definisjon()
        log.info("Forsøker løse avklaringsbehov[${definisjoner}] på behandling[${behandling.referanse}]")

        avklaringsbehovene.validateTilstand(
            behandling = behandling,
            avklaringsbehov = definisjoner,
        )

        // løses det behov som fremtvinger tilbakehopp?
        val flytOrkestrator = FlytOrkestrator(connection)
        flytOrkestrator.forberedLøsingAvBehov(definisjoner, behandling, kontekst)

        // Bør ideelt kalle på
        løsFaktiskAvklaringsbehov(kontekst, avklaringsbehovene, avklaringsbehov)
    }


    @Suppress("UNCHECKED_CAST")
    private fun løsFaktiskAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene,
        it: AvklaringsbehovLøsning
    ) {
        // Liker denne casten fryktelig lite godt -_- men må til pga generics *
        val avklaringsbehovsLøser =
            avklaringsbehovsLøsere.getValue(it.definisjon()) as AvklaringsbehovsLøser<AvklaringsbehovLøsning>
        val løsningsResultat = avklaringsbehovsLøser.løs(kontekst = kontekst, løsning = it)

        avklaringsbehovene.leggTilFrivilligHvisMangler(it.definisjon())

        avklaringsbehovene.løsAvklaringsbehov(
            it.definisjon(),
            løsningsResultat.begrunnelse,
            "Saksbehandler", // TODO: Hente fra context
            løsningsResultat.kreverToTrinn
        )
    }
}
