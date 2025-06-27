package no.nav.aap.behandlingsflyt.behandling.klage.andreinstans

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.Rolle
import java.time.LocalDate

class AndreinstansService(
    private val klageresultatUtleder: KlageresultatUtleder,
    private val andreinstansGateway: AndreinstansGateway,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val ansattInfoService: AnsattInfoService
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        andreinstansGateway = gatewayProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        ansattInfoService = AnsattInfoService()
    )

    fun oversendTilAndreinstans(klageBehandlingId: BehandlingId) {
        val klageBehandling = behandlingRepository.hent(klageBehandlingId)
        val sak = sakRepository.hent(klageBehandling.sakId)
        val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(klageBehandlingId)
        val klage = mottattDokumentRepository.hentDokumenterAvType(
            klageBehandlingId,
            InnsendingType.KLAGE
        )
        val avklarinsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(klageBehandlingId)
        val besluttersEnhet = if (Miljø.erDev() || Miljø.erLokal()) {
            "0300" // Det finnes ikke testdata i NOM - bruker hardkodet enhet i dev
        } else {
            val beslutter = utledBeslutter(avklarinsbehovene)
            ansattInfoService.hentAnsattEnhet(beslutter)
        }
        requireNotNull(besluttersEnhet) {
            "Fant ikke beslutters enhet"
        }

        andreinstansGateway.oversendTilAndreinstans(
            saksnummer = sak.saksnummer,
            behandlingsreferanse = klageBehandling.referanse,
            kravDato = utledKravdato(klage.single()),
            klagenGjelder = sak.person,
            klageresultat = klageresultat,
            saksbehandlersEnhet = besluttersEnhet
        )
    }

    private fun utledKravdato(dokument: MottattDokument): LocalDate {
        require(dokument.type == InnsendingType.KLAGE) {
            "Kravdato kan kun utledes fra klage"
        }
        val data = dokument.strukturerteData<KlageV0>()?.data
        requireNotNull(data) {
            "Kravdato kan ikke utledes fra klage uten strukturert data"
        }
        return data.kravMottatt
    }

    private fun utledBeslutter(avklaringsbehovene: Avklaringsbehovene): String {
        val definisjoner = Definisjon.entries.filter { it.løsesAv.contains(Rolle.BESLUTTER) }
        return avklaringsbehovene.hentBehovForDefinisjon(definisjoner).mapNotNull { avklaringsbehov ->
            avklaringsbehov.historikk.filter { it.endretAv.erNavIdent() && it.status == Status.AVSLUTTET }.maxOrNull()
        }.max().endretAv
    }

    private fun String.erNavIdent(): Boolean {
        return this.matches(Regex("\\w\\d{6}"))
    }

}