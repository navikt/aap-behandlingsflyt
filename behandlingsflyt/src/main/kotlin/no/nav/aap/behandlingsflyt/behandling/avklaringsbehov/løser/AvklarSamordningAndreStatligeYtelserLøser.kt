package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningAndreStatligeYtelserLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

class AvklarSamordningAndreStatligeYtelserLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSamordningAndreStatligeYtelserLøsning> {

    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val samordningAndreStatligeYtelserRepository = repositoryProvider.provide<SamordningAndreStatligeYtelserRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningAndreStatligeYtelserLøsning
    ): LøsningsResultat {
        samordningAndreStatligeYtelserRepository.lagre(
            kontekst.behandlingId(),
            SamordningAndreStatligeYtelserVurdering(
                begrunnelse = løsning.samordningAndreStatligeYtelserVurdering.begrunnelse,
                vurdertAv = kontekst.bruker.ident,
                vurderingPerioder = løsning.samordningAndreStatligeYtelserVurdering.vurderingPerioder.map {
                    SamordningAndreStatligeYtelserVurderingPeriode(
                        ytelse = it.ytelse,
                        periode = it.periode,
                        beløp = it.beløp
                    )
                }
            )
        )
        return LøsningsResultat("Vurdert samordning andre statlige ytelser")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER
    }
}