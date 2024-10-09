package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import java.util.UUID

class Brevbestiling(val id: Long, val typeBrev: TypeBrev, val referanse: UUID, val status: Status)