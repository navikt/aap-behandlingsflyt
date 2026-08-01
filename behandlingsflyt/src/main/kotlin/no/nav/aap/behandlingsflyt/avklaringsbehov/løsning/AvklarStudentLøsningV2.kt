package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarStudentLøserV2
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.steg.student.PeriodisertStudentDto
import no.nav.aap.behandlingsflyt.steg.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_STUDENT_KODE_V2
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider


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
) : PeriodisertAvklaringsbehovLøsning<PeriodisertStudentDto> {
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
}

