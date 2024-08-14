package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate.BarnetilleggGrunnlagDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate.FolkeregistrertBarnDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate.ManuelleBarnVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate.ManueltBarnDto
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class ManuelleBarnService(
    private val manuelleBarnVurderingRepository: ManuellebarnVurderingRepository,
    private val barnRepository: BarnRepository,
    private val personinfoGateway: PersoninfoGateway
) {

    fun samleManuelleBarnGrunnlag(behandlingId: BehandlingId, token: OidcToken): BarnetilleggGrunnlagDto {

        val folkeregistrerteBarn = hentFolkeregistrerteBarn(behandlingId, token)
        val manuelleBarn = hentManuelleBarn(behandlingId, token)
        val manuelleBarnVurdering = manuelleBarnVurderingRepository.hentHvisEksisterer(behandlingId)?.vurdering

        return BarnetilleggGrunnlagDto(
            manueltOppgitteBarn = listOf( // TODO ikke bruk hardkodede verdier
                ManueltBarnDto("Pelle Potet", Ident("12345678912")),
                ManueltBarnDto("Kåre Kålrabi", Ident("12121212121"))
            ),
            folkeregistrerteBarn = folkeregistrerteBarn,
            vurdering = manuelleBarnVurdering?.let { ManuelleBarnVurderingDto.toDto(it) }
        )
    }

    private fun hentFolkeregistrerteBarn(behandlingId: BehandlingId, token: OidcToken):List<FolkeregistrertBarnDto> {
        val folkeregisterBarn = barnRepository.hent(behandlingId)

        return folkeregisterBarn.barn.map { barn ->
            val barnPersoninfo =
                personinfoGateway.hentPersoninfoForIdent(barn.ident, token)
            FolkeregistrertBarnDto(
                navn = barnPersoninfo.fultNavn(),
                ident = barnPersoninfo.ident,
                forsorgerPeriode = barn.periodeMedRettTil()
            )
        }
    }

    private fun hentManuelleBarn(behandlingId: BehandlingId, token: OidcToken): List<ManueltBarnDto> {
        val manueltOppgitteBarn = emptyList<Barn>() // TODO bruk repositroy for manuelle barn

        return manueltOppgitteBarn.map { barn ->
            val barnPersoninfo =
                personinfoGateway.hentPersoninfoForIdent(barn.ident, token)
            ManueltBarnDto(
                navn = barnPersoninfo.fultNavn(),
                ident = barnPersoninfo.ident,
            )
        }
    }

}