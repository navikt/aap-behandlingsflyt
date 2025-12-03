package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

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
            ÅrData(
                år = manuellInntekt.år.value,
                beløp = manuellInntekt.belop?.verdi,
                eøsBeløp = manuellInntekt.eøsBeløp?.verdi
            )
        }

        return ManuellInntektGrunnlagVurdering(
            begrunnelse = manuelleInntekter.first().begrunnelse,
            vurdertAv = VurdertAvResponse(
                manuelleInntekter.first().vurdertAv,
                manuelleInntekter.first().opprettet.toLocalDate()
            ),
            årsVurderinger = årsVurderinger
        )
    }

    fun mapHistoriskeManuelleVurderinger(behandlingsreferanse: BehandlingReferanse): List<ManuellInntektGrunnlagVurdering> {
        val behandling = behandlingRepository.hent(behandlingsreferanse.referanse.let(::BehandlingReferanse))

        val historiskeVurderinger =
            manuellInntektGrunnlagRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)

        val mappedHistoriskeVurderinger = historiskeVurderinger.map { historiskManuellInntektSet ->
            historiskManuellInntektSet

            val årsVurderinger = historiskManuellInntektSet.map { historiskManuellInntekt ->
                ÅrData(
                    år = historiskManuellInntekt.år.value,
                    beløp = historiskManuellInntekt.belop?.verdi,
                    eøsBeløp = historiskManuellInntekt.eøsBeløp?.verdi
                )
            }

            ManuellInntektGrunnlagVurdering(
                begrunnelse = historiskManuellInntektSet.first().begrunnelse,
                vurdertAv = VurdertAvResponse(
                    historiskManuellInntektSet.first().vurdertAv,
                    historiskManuellInntektSet.first().opprettet.toLocalDate()
                ),
                årsVurderinger = årsVurderinger
            )
        }
        return mappedHistoriskeVurderinger
    }
}