package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningGraderingLøser(
    private val samordningYtelseVurderingRepository: SamordningVurderingRepository,
    private val samordningYtelseRepository: SamordningYtelseRepository,
) : AvklaringsbehovsLøser<AvklarSamordningGraderingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningYtelseVurderingRepository = repositoryProvider.provide(),
        samordningYtelseRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSamordningGraderingLøsning): LøsningsResultat {
        val samordningService = SamordningService(samordningYtelseVurderingRepository, samordningYtelseRepository)
        val vurderingerForSamordning = løsning.vurderingerForSamordning
        val samordningYtelseGrunnlag = samordningYtelseRepository.hentHvisEksisterer(kontekst.behandlingId())
        val samordningsvurderinger = SamordningVurderingGrunnlag(
            begrunnelse = vurderingerForSamordning.begrunnelse,
            maksDatoEndelig = vurderingerForSamordning.maksDatoEndelig,
            maksDato = vurderingerForSamordning.maksDato,
            vurderinger = vurderingerForSamordning.vurderteSamordningerData.groupBy { it.ytelseType }.map { it ->
                SamordningVurdering(
                    ytelseType = it.key,
                    vurderingPerioder = it.value.map { vurdering ->
                        SamordningVurderingPeriode(
                            periode = vurdering.periode,
                            gradering = vurdering.gradering?.let(::Prosent),
                            kronesum = vurdering.kronesum,
                            manuell = vurdering.manuell,
                        )
                    },
                )
            })

        val perioderSomIkkeHarBlittVurdert = samordningService.perioderSomIkkeHarBlittVurdert(
            samordningYtelseGrunnlag, samordningService.tidligereVurderinger(samordningsvurderinger)
        )

        if (perioderSomIkkeHarBlittVurdert.isNotEmpty()) {
            throw UgyldigForespørselException(message = "Har ikke vurdert alle perioder for samordning med andre folketrygdytelser")
        }

        samordningYtelseVurderingRepository.lagreVurderinger(
            kontekst.kontekst.behandlingId, samordningsvurderinger
        )

        if (kontekst.kontekst.behandlingType == TypeBehandling.Revurdering && vurderingerForSamordning.maksDatoEndelig != true && vurderingerForSamordning.maksDato != null) {
            throw UgyldigForespørselException(message = "Virkningstidspunkt må være bekreftet for å gå videre i behandlingen.")
        }

        return LøsningsResultat("Vurdert samordning")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_GRADERING
    }
}