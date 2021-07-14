package org.jlleitschuh.gradle.ktlint.testdsl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Has to be java annotation
// Workaround for https://youtrack.jetbrains.com/issue/IDEA-265284
@ArgumentsSource(GradleArgumentsProvider.class)
@ParameterizedTest(name = "Gradle {0}: {displayName}")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommonTest {}
