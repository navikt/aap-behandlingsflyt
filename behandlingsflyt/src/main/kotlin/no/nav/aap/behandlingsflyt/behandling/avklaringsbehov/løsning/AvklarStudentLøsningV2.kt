package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarStudentLøserV2
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.PeriodisertStudentDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_STUDENT_KODE_V2
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_STUDENT_KODE_V2)
class AvklarStudentLøsningV2(
    @param:JsonProperty(
        "løsningerForPerioder",
        required = true
    ) override val løsningerForPerioder: List<PeriodisertStudentDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_STUDENT_KODE_V2
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5037`
) : PeriodisertAvklaringsbehovLøsning<PeriodisertStudentDto>, LøsningMedPeriodiserteVurderinger {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarStudentLøserV2(repositoryProvider).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<StudentRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.somStudenttidslinje() ?: Tidslinje<Unit>()
    }

    override fun hentVurderinger(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): List<PeriodisertVurdering> {
        val repository = repositoryProvider.provide<StudentRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty().toList()
    }

    override fun somVurderinger(bruker: Bruker, behandlingId: BehandlingId): List<StudentVurdering> {
        return løsningerForPerioder.map { it.tilStudentVurdering(bruker, behandlingId) }
    }
}

