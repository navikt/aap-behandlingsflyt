package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingMetadata
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory


class VarsleOppgaveOmHendelseJobbUtFører private constructor(
    private val oppgavestyringGateway: OppgavestyringGateway,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()

        log.info("Varsler hendelse til OppgaveStyring. ${hendelse.saksnummer} :: ${hendelse.referanse.referanse}")
        val oppdatertHendelse =
            if (erFørstegangsbehandlingMedKunAvslagEtterSykdomsvilkår(hendelse, BehandlingId(input.behandlingId()))) {
                hendelse.copy(behandlingMetadata = BehandlingMetadata.AVSLAG_11_5_FØRSTEGANGSBEHANDLING)
            } else hendelse
        oppgavestyringGateway.varsleHendelse(oppdatertHendelse)
    }

    private fun erFørstegangsbehandlingMedKunAvslagEtterSykdomsvilkår(
        hendelse: BehandlingFlytStoppetHendelse,
        behandlingId: BehandlingId
    ): Boolean {
        /**
         * TODO: Hvordan skal vi agere hvis det har vært en studentvurdering, men "Nei"
         */
        if (hendelse.behandlingType == TypeBehandling.Førstegangsbehandling && !harStudentVurdering(behandlingId)) {
            val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
            if (sykdomsGrunnlag == null || sykdomsGrunnlag.sykdomsvurderinger.isEmpty()) return false

            val erSykdomDefinitivtAvslag = sykdomsGrunnlag.sykdomsvurderinger.all { sykdomsvurdering ->
                !sykdomsvurdering.erOppfyltOrdinærMedUtlededeFelter() && !sykdomsvurdering.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() && !sykdomsvurdering.skalVurderesForSykepengeerstatning()
            }
            return erSykdomDefinitivtAvslag
        }
        return false
    }

    private fun harStudentVurdering(behandlingId: BehandlingId): Boolean {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
        return studentGrunnlag != null && studentGrunnlag.vurderinger?.isNotEmpty() == true
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return VarsleOppgaveOmHendelseJobbUtFører(
                gatewayProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
            )
        }

        override val type = "flyt.hendelse"
        override val navn = "Oppgavestyringshendelse"
        override val beskrivelse = "Produsere hendelse til oppgavestyring"
    }
}
