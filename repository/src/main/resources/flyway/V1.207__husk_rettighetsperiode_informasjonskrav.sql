alter table informasjonskrav_oppdatert
add column rettighetsperiode daterange null;

-- Kan settes not null når ingen åpne behandlinger har null.
