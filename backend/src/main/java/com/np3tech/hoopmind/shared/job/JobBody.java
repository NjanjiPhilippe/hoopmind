package com.np3tech.hoopmind.shared.job;

/** Le travail d'un job ; son résultat est enregistré en JSON. */
@FunctionalInterface
public interface JobBody<R> {

    R run(JobContext context);
}
