package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.MonthDay
import java.time.Year


class ManglendeInntektGrunnlagService(
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val behandlingRepository: BehandlingRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    fun mapManuellVurderinger(behandlingsreferanse: BehandlingReferanse): ManuellInntektGrunnlagVurdering? {
        val behandling = behandlingRepository.hent(behandlingsreferanse.referanse.let(::BehandlingReferanse))

        val grunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandling.id)
        if (grunnlag == null) return null

        val manuelleInntekter = grunnlag.manuelleInntekter

        val årsVurderinger = manuelleInntekter.map { manuellInntekt ->
            val gVerdi = Grunnbeløp.tilTidslinjeGjennomsnitt().segment(
                Year.of(manuellInntekt.år.value).atMonthDay(
                    MonthDay.of(1, 1)
                )
            )!!.verdi

            AarData(
                ar = manuellInntekt.år.value,
                belop = manuellInntekt.belop.verdi,
                gverdi = gVerdi.verdi
            )
        }

        return ManuellInntektGrunnlagVurdering(
            begrunnelse = manuelleInntekter.first().begrunnelse,
            aarsak = manuelleInntekter.first().aarsak?.name,
            vurdertAv = VurdertAvResponse(
                manuelleInntekter.first().vurdertAv,
                manuelleInntekter.first().opprettet.toLocalDate()
            ),
            aarsVurderinger = årsVurderinger
        )
    }

    fun mapHistoriskeManuelleVurderinger(behandlingsreferanse: BehandlingReferanse): List<ManuellInntektGrunnlagVurdering> {
        val behandling = behandlingRepository.hent(behandlingsreferanse.referanse.let(::BehandlingReferanse))

        val historiskeVurderinger =
            manuellInntektGrunnlagRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)

        val mappedHistoriskeVurderinger = historiskeVurderinger.map { historiskManuellInntektSet ->
            historiskManuellInntektSet

            val aarsVurderinger = historiskManuellInntektSet.map { historiskManuellInntekt ->
                val gVerdi = Grunnbeløp.tilTidslinjeGjennomsnitt().segment(
                    Year.of(historiskManuellInntekt.år.value).atMonthDay(
                        MonthDay.of(1, 1)
                    )
                )!!.verdi

                AarData(
                    ar = historiskManuellInntekt.år.value,
                    belop = historiskManuellInntekt.belop.verdi,
                    gverdi = gVerdi.verdi
                )
            }

            ManuellInntektGrunnlagVurdering(
                begrunnelse = historiskManuellInntektSet.first().begrunnelse,
                aarsak = historiskManuellInntektSet.first().aarsak?.name,
                vurdertAv = VurdertAvResponse(
                    historiskManuellInntektSet.first().vurdertAv,
                    historiskManuellInntektSet.first().opprettet.toLocalDate()
                ),
                aarsVurderinger = aarsVurderinger
            )
        }
        return mappedHistoriskeVurderinger
    }
}