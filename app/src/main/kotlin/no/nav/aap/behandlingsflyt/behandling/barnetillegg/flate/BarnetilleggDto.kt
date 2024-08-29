package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnetilleggDto(val oppgitteBarn: List<Ident>, val folkeregisterbarn: List<IdentifiserteBarnDto>, val barnSomTrengerVurdering: List<IdentifiserteBarnDto>)