package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
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

    fun mapManuellVurderinger(
        behandlingsreferanse: BehandlingReferanse,
        vurdertAvService: VurdertAvService,
    ): ManuellInntektGrunnlagVurdering? {
        val behandling = behandlingRepository.hent(behandlingsreferanse.referanse.let(::BehandlingReferanse))

        val grunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandling.id)
        if (grunnlag == null || grunnlag.manuelleInntekter.isEmpty()) return null

        val manuelleInntekter = grunnlag.manuelleInntekter

        val årsVurderinger = manuelleInntekter.map { manuellInntekt ->
            ÅrData(
                år = manuellInntekt.år.value,
                beløp = manuellInntekt.belop?.verdi,
                eøsBeløp = manuellInntekt.eøsBeløp?.verdi,
                ferdigLignetPGI = manuellInntekt.ferdigLignetPGI?.verdi
            )
        }

        return ManuellInntektGrunnlagVurdering(
            begrunnelse = manuelleInntekter.first().begrunnelse,
            vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                definisjon = Definisjon.FASTSETT_MANUELL_INNTEKT,
                behandlingId = behandling.id,
                vurdertAv = vurdertAvService.medNavnOgEnhet(
                    ident = manuelleInntekter.first().vurdertAv,
                    tidspunkt = manuelleInntekter.first().opprettet,
                ),
            ),
            årsVurderinger = årsVurderinger
        )
    }

    fun mapHistoriskeManuelleVurderinger(
        behandlingsreferanse: BehandlingReferanse,
        vurdertAvService: VurdertAvService,
    ): List<ManuellInntektGrunnlagVurdering> {
        val behandling = behandlingRepository.hent(behandlingsreferanse.referanse.let(::BehandlingReferanse))
        val historiskeVurderinger = behandlingRepository.hentAlleFor(
            behandling.sakId,
            TypeBehandling.ytelseBehandlingstyper(),
        ).filter { it.id != behandling.id }
            .mapNotNull { historiskBehandling ->
                manuellInntektGrunnlagRepository.hentHvisEksisterer(historiskBehandling.id)
                    ?.manuelleInntekter
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { historiskBehandling.id to it }
            }

        val mappedHistoriskeVurderinger = historiskeVurderinger
            .sortedByDescending { it.second.first().opprettet }
            .map { (behandlingId, historiskManuellInntektSet) ->

            val årsVurderinger = historiskManuellInntektSet.map { historiskManuellInntekt ->
                ÅrData(
                    år = historiskManuellInntekt.år.value,
                    beløp = historiskManuellInntekt.belop?.verdi,
                    eøsBeløp = historiskManuellInntekt.eøsBeløp?.verdi,
                    ferdigLignetPGI = historiskManuellInntekt.ferdigLignetPGI?.verdi
                )
            }

            ManuellInntektGrunnlagVurdering(
                begrunnelse = historiskManuellInntektSet.first().begrunnelse,
                vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                    definisjon = Definisjon.FASTSETT_MANUELL_INNTEKT,
                    behandlingId = behandlingId,
                    vurdertAv = vurdertAvService.medNavnOgEnhet(
                        ident = historiskManuellInntektSet.first().vurdertAv,
                        tidspunkt = historiskManuellInntektSet.first().opprettet,
                    ),
                ),
                årsVurderinger = årsVurderinger
            )
        }

        return mappedHistoriskeVurderinger
    }
}