package org.assertj.core.api

import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

fun ObjectAssert<BuildTask?>.hasOutcome(outcome: TaskOutcome) {
    this.objects.assertNotNull(this.info, this.actual)
    this.objects.assertEqual(this.info, this.actual!!.outcome, outcome)
}
