/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.types.StubTypeForBuilderInference
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.BasicExpressionTypingVisitor
import org.jetbrains.kotlin.types.isError

object BuilderInferenceAssignmentChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is PropertyDescriptor) return
        if (resolvedCall.candidateDescriptor.returnType !is StubTypeForBuilderInference) return
        if (reportOn !is KtNameReferenceExpression) return
        val binaryExpression = reportOn.getParentOfType<KtBinaryExpression>(strict = true) ?: return
        if (!BasicExpressionTypingVisitor.isLValue(reportOn, binaryExpression)) return

        val leftType = resultingDescriptor.returnType?.takeIf { !it.isError } ?: return
        val right = binaryExpression.right ?: return
        val rightType = right.getType(context.trace.bindingContext) ?: return

        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(rightType, leftType)) {
            val dfi = context.dataFlowInfo
            val dfvFactory = context.dataFlowValueFactory
            val stableTypesFromDataFlow = dfi.getStableTypes(
                dfvFactory.createDataFlowValue(right, rightType, context.trace.bindingContext, context.moduleDescriptor),
                context.languageVersionSettings
            )
            if (stableTypesFromDataFlow.none {
                    KotlinTypeChecker.DEFAULT.isSubtypeOf(it, leftType)
                }
            ) {
                context.trace.report(Errors.TYPE_MISMATCH.on(right, leftType, rightType))
            }
        }
    }
}
