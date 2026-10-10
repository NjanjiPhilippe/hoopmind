package com.np3tech.hoopmind.ml.spark;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Les beans Spark ne sont créés que si {@code hoopmind.ml.spark.enabled} vaut {@code true} (valeur par défaut).
 * Désactiver Spark permet de démarrer l'API sans moteur de calcul (tests, démonstration légère).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnProperty(prefix = "hoopmind.ml.spark", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnSpark {
}
