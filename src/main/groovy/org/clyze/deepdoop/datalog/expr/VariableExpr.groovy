package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor

@Canonical
@ToString(includePackage = false)
class VariableExpr implements IExpr {

	String name

	def isDontCare() { name == '_' }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	static List<VariableExpr> genTempVars(int n) {
		(0..<n).collect { new VariableExpr("v$it") }
	}
}
