package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class HåndterUbehandledeMeldekortForSakJobbUtfører(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val unleashGateway: UnleashGateway
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.UbehandledeMeldekortJobb)) {
            return
        }

        val sakId = SakId(input.sakId())
        val ubehandledeDokumenter =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, InnsendingType.MELDEKORT)

        log.info("Fant ${ubehandledeDokumenter.size} ubehandlede meldekort for sak $sakId")
        ubehandledeDokumenter.forEach { dokument ->
            flytJobbRepository.leggTil(
                HåndterUbehandletDokumentJobbUtfører.nyJobb(
                    dokument.sakId,
                    dokument.referanse
                )
            )
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return HåndterUbehandledeDokumenterJobbUtfører(
                mottattDokumentRepository = repositoryProvider.provide(),
                flytJobbRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide()
            )
        }

        override val type = "flyt.HåndterUbehandledeMeldekortForSak"

        override val navn = "Håndter dokumenter ubehandlede meldekort for sak"

        override val beskrivelse =
            "Sjekker om det finnes ubehandlede meldekort på en sak og håndterer disse."

        fun nyJobb(sakId: SakId) =
            JobbInput(HåndterUbehandledeMeldekortForSakJobbUtfører)
                .apply {
                    forSak(sakId.toLong())
                }

    }
}