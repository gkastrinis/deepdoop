package org.clyze.deepdoop.actions.tranform

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.SymbolTableVisitingActor
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr

import static org.clyze.deepdoop.datalog.Annotation.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.TYPEVALUES
import static org.clyze.deepdoop.datalog.element.relation.Type.TYPE_STRING
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1

@Canonical
class TypesTransformer extends DummyTransformer {

	SymbolTableVisitingActor symbolTable

	void enter(BlockLvl0 n) {
		symbolTable.rootTypes.each { root ->
			extraRelDecls << new RelDeclaration(new Constructor(mkCon(root), []), [TYPE_STRING, root], [CONSTRUCTOR] as Set)
		}
	}

	IVisitable exit(TypeDeclaration n, Map m) {
		if (TYPEVALUES in n.annotations) {
			def rootT = symbolTable.typeToRootType[n.type]
			n.annotations.find { it == TYPEVALUES }.args.each { key, value ->
				def rel = new Relation("${n.type.name}:$key", [var1()])
				def con = new Constructor(mkCon(rootT), [value, var1()])
				extraRelDecls << new RelDeclaration(rel, [n.type])
				extraRules << new Rule(new LogicalElement([new ConstructionElement(con, n.type), rel]), null)
			}
		}
		return n
	}

	static def mkCon(Type t) { "${t.name}:byStr" }

	// Overrides to avoid unneeded allocations

	IVisitable exit(RelDeclaration n, Map m) { n }

	IVisitable exit(LogicalElement n, Map m) { n }

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(ConstructionElement n, Map m) { n }

	IVisitable exit(Constructor n, Map m) { n }

	IVisitable exit(Relation n, Map m) { n }

	IVisitable exit(Type n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}