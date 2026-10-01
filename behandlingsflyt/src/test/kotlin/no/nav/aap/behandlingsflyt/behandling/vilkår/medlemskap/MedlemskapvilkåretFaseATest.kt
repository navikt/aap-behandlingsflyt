package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

// Testskjelett for Fase A (rød sone). Fylles ut og aktiveres av utvikler sammen med A2/A3.
@Disabled("TODO Fase A: implementer A2/A3 og fyll ut testene")
class MedlemskapvilkåretFaseATest {

    @Test
    fun `automatiske vurderinger i grunnlaget gir samme vilkårsutfall som uten dem`() {
        TODO("Bygg grunnlag med og uten lagret automatisk vurdering (SYSTEMBRUKER); assert lik utfall og manuellVurdering=false")
    }

    @Test
    fun `manuell vurdering vinner over automatisk for samme periode`() {
        TODO("Grunnlag med både automatisk og manuell vurdering; assert at manuell brukes i vilkåret (manuellVurdering=true)")
    }

    @Test
    fun `kun automatiske vurderinger og ingen nyeSoknadGrunnlag gir IKKE_RELEVANT`() {
        TODO("Assert at grenen for IKKE_RELEVANT ikke hoppes over når vurderinger kun er automatiske")
    }

    @Test
    fun `automatisk vurdering lagres kun ved kanBehandlesAutomatisk og ikke på nytt uten diff`() {
        TODO("Gjelder VurderLovvalgSteg (A2): lagre NOR+medlem for hele rettighetsperioden; ingenting ved false; ingen ny lagring uten diff")
    }

    @Test
    fun `perioderSomIkkeErTilstrekkeligVurdert teller ikke automatiske vurderinger som manuelle`() {
        TODO("Gjelder VurderLovvalgSteg (A3)")
    }
}
