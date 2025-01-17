/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

public abstract class KtExpressionInfoProvider : KtAnalysisSessionComponent() {
    public abstract fun getReturnExpressionTargetSymbol(returnExpression: KtReturnExpression): KtCallableSymbol?
    public abstract fun getWhenMissingCases(whenExpression: KtWhenExpression): List<WhenMissingCase>
    public abstract fun isUsedAsExpression(expression: KtExpression): Boolean
}

public interface KtExpressionInfoProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtReturnExpression.getReturnTargetSymbol(): KtCallableSymbol? =
        withValidityAssertion { analysisSession.expressionInfoProvider.getReturnExpressionTargetSymbol(this) }

    public fun KtWhenExpression.getMissingCases(): List<WhenMissingCase> =
        withValidityAssertion { analysisSession.expressionInfoProvider.getWhenMissingCases(this) }

    /**
     * Compute if the value of a given expression is possibly used. Or,
     * conversely, compute whether the value of an expression is *not* safe to
     * discard.
     *
     * E.g. `x` in the following examples *are* used (`x.isUsedAsExpression() == true`)
     *   - `if (x) { ... } else { ... }`
     *   - `val a = x`
     *   - `x + 8`
     *   - `when (x) { 1 -> ...; else -> ... }
     *
     * E.g. `x` in the following example is definitely *not* used (`x.isUsedAsExpression() == false`)
     *   - `run { x; println(50) }`
     *   - `when (x) { else -> ... }`
     *
     * **Note!** This is a conservative check, not a control-flow analysis.
     * E.g. `x` in the following example *is possibly used*, even though the
     * value is never consumed at runtime.
     *   - `x + try { throw Exception() } finally { return }`
     *
     */
    public fun KtExpression.isUsedAsExpression(): Boolean =
        withValidityAssertion { analysisSession.expressionInfoProvider.isUsedAsExpression(this) }
}