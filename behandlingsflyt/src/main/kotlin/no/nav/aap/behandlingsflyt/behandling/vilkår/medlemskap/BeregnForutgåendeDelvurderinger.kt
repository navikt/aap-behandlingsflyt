package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapFaktaGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.etterlevelse.sporing.Sporbar
import no.nav.aap.etterlevelse.sporing.Sporingsnøkkel
import java.time.LocalDate

/** Alt som påvirker hvilke delvurderinger som lagres: både nye data og tidligere vurderinger. */
data class ForutgåendeDelvurderingerSporingGrunnlag(
    val behandlingId: BehandlingId,
    val relevanteDatoer: List<LocalDate>,
    val forrigeBehandlingsVurderinger: List<ForutgåendeDelvurderingerGrunnlag>,
    val eksisterendeVurderinger: List<ForutgåendeDelvurderingerGrunnlag>,
    val register: ForutgåendeMedlemskapFaktaGrunnlag,
)

/**
 * Utleder delvurderingene som skal lagres, med samme regler som [BeregnLovvalgDelvurderinger]:
 * - En dato som fantes i forrige behandling og ikke er nyeste dato, gjenbruker forrige behandlings vurdering.
 * - Alle andre datoer (nye datoer og nyeste dato) beregnes på nytt.
 * - Gir en ny beregning samme vurdering som allerede ligger i behandlingen, beholdes den eksisterende,
 *   slik at [ForutgåendeDelvurderingerGrunnlag.vurdertIBehandling] ikke endres uten at vurderingen endres.
 *
 * Hver dato vurderes mot vinduet [[dato] - 5 år, [dato]].
 */
object BeregnForutgåendeDelvurderinger :
    Sporbar<ForutgåendeDelvurderingerSporingGrunnlag, List<ForutgåendeDelvurderingerGrunnlag>> {
    override val sporingsnøkkel = Sporingsnøkkel("beregn-forutgående-delvurderinger")

    override fun kjørMedSporing(
        grunnlag: ForutgåendeDelvurderingerSporingGrunnlag
    ): List<ForutgåendeDelvurderingerGrunnlag> {
        val relevanteDatoer = grunnlag.relevanteDatoer.sorted()
        val nyesteDato = relevanteDatoer.last()
        val forrige = grunnlag.forrigeBehandlingsVurderinger.associateBy { it.relevantDato }
        val eksisterende = grunnlag.eksisterendeVurderinger.associateBy { it.relevantDato }

        return relevanteDatoer.map { dato ->
            val fraForrigeBehandling = forrige[dato]
            if (fraForrigeBehandling != null && dato != nyesteDato) {
                return@map fraForrigeBehandling
            }

            val vurdering = ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag.register, dato)

            val eksisterendeForDato = eksisterende[dato]
            if (eksisterendeForDato?.vurdering() == vurdering) {
                eksisterendeForDato
            } else {
                ForutgåendeDelvurderingerGrunnlag(
                    kanBehandlesAutomatisk = vurdering.kanBehandlesAutomatisk,
                    tilhørighetVurdering = vurdering.tilhørighetVurdering,
                    relevantDato = dato,
                    vurdertIBehandling = grunnlag.behandlingId,
                )
            }
        }
    }
}
