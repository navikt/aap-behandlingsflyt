package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgRegisterGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.etterlevelse.sporing.Sporbar
import no.nav.aap.etterlevelse.sporing.Sporingsnøkkel
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

/** Alt som påvirker hvilke delvurderinger som lagres: både nye data og tidligere vurderinger. */
data class LovvalgDelvurderingerSporingGrunnlag(
    val behandlingId: BehandlingId,
    val relevanteDatoer: List<LocalDate>,
    val forrigeBehandlingsVurderinger: List<LovvalgDelvurderingerGrunnlag>,
    val eksisterendeVurderinger: List<LovvalgDelvurderingerGrunnlag>,
    val register: MedlemskapLovvalgRegisterGrunnlag,
    val rettighetsperiodeTom: LocalDate,
)

/**
 * Utleder delvurderingene som skal lagres:
 * - En dato som fantes i forrige behandling og ikke er nyeste dato, gjenbruker forrige behandlings vurdering.
 * - Alle andre datoer (nye datoer og nyeste dato) beregnes på nytt.
 * - Gir en ny beregning samme vurdering som allerede ligger i behandlingen, beholdes den eksisterende,
 *   slik at [LovvalgDelvurderingerGrunnlag.vurdertIBehandling] ikke endres uten at vurderingen endres.
 */
object BeregnLovvalgDelvurderinger :
    Sporbar<LovvalgDelvurderingerSporingGrunnlag, List<LovvalgDelvurderingerGrunnlag>> {
    override val sporingsnøkkel = Sporingsnøkkel("beregn-lovvalg-delvurderinger")

    override fun kjørMedSporing(grunnlag: LovvalgDelvurderingerSporingGrunnlag): List<LovvalgDelvurderingerGrunnlag> {
        val relevanteDatoer = grunnlag.relevanteDatoer.sorted()
        val nyesteDato = relevanteDatoer.last()
        val forrige = grunnlag.forrigeBehandlingsVurderinger.associateBy { it.relevantDato }
        val eksisterende = grunnlag.eksisterendeVurderinger.associateBy { it.relevantDato }

        return relevanteDatoer.map { dato ->
            val fraForrigeBehandling = forrige[dato]
            if (fraForrigeBehandling != null && dato != nyesteDato) {
                return@map fraForrigeBehandling
            }

            val vurdering = LovvalgMedlemskapVurderingService.vurderTilhørighet(
                grunnlag.register,
                Periode(dato, maxOf(dato, grunnlag.rettighetsperiodeTom)),
            )

            val eksisterendeForDato = eksisterende[dato]
            if (eksisterendeForDato?.vurdering() == vurdering) {
                eksisterendeForDato
            } else {
                LovvalgDelvurderingerGrunnlag(
                    kanBehandlesAutomatisk = vurdering.kanBehandlesAutomatisk,
                    tilhørighetVurdering = vurdering.tilhørighetVurdering,
                    relevantDato = dato,
                    vurdertIBehandling = grunnlag.behandlingId,
                )
            }
        }
    }
}
