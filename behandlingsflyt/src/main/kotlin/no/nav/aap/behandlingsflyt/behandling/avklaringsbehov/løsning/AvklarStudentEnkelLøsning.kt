package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarStudentLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.PeriodisertStudentDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_STUDENT_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_STUDENT_KODE)
class AvklarStudentEnkelLøsning(
    @param:JsonProperty("studentvurdering", required = true) val studentvurdering: StudentVurderingDTO,
    @param:JsonProperty(
        "løsningerForPerioder",
        required = true
    ) val løsningerForPerioder: Set<PeriodisertStudentDto>? = null,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_STUDENT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5001`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarStudentLøser(repositoryProvider).løs(kontekst, this)
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_STUDENT_KODE)
class AvklarStudentLøsning(
    @param:JsonProperty(
        "løsningerForPerioder",
        required = true
    ) override val løsningerForPerioder: List<PeriodisertStudentDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_STUDENT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5001`
) : PeriodisertAvklaringsbehovLøsning<PeriodisertStudentDto> {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarStudentLøser(repositoryProvider).løs(kontekst, this.tilEnkeltLøsning())
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<StudentRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.somStudenttidslinje() ?: Tidslinje<Unit>()
    }
    
    private fun tilEnkeltLøsning(): AvklarStudentEnkelLøsning {
        return AvklarStudentEnkelLøsning(
            studentvurdering = this.løsningerForPerioder.first().tilGammelDto(),
            løsningerForPerioder = this.løsningerForPerioder.toSet(),
            behovstype = this.behovstype
        )
    }
}

