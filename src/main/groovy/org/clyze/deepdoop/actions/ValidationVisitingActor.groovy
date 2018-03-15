package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.system.Error

import static org.clyze.deepdoop.datalog.Annotation.*
import static org.clyze.deepdoop.system.Error.error as error
import static org.clyze.deepdoop.system.Error.warn as warn
import static org.clyze.deepdoop.system.SourceManager.recallStatic as recall

class ValidationVisitingActor extends DefaultVisitor<IVisitable> implements TDummyActor<IVisitable> {

	TypeInfoVisitingActor typeInfoActor
	RelationInfoVisitingActor relInfoActor
	ConstructionInfoVisitingActor conInfoActor

	Set<String> tmpDeclaredRelations = [] as Set
	Set<String> tmpDeclaredTypes = [] as Set
	Map<String, Integer> arities = [:]

	ValidationVisitingActor(TypeInfoVisitingActor typeInfoActor, RelationInfoVisitingActor relInfoActor, ConstructionInfoVisitingActor conInfoActor) {
		actor = this
		this.typeInfoActor = typeInfoActor
		this.relInfoActor = relInfoActor
		this.conInfoActor = conInfoActor
	}

	IVisitable exit(BlockLvl2 n, Map m) { n }

	IVisitable visit(BlockLvl1 n) { throw new UnsupportedOperationException() }

	void enter(RelDeclaration n) {
		if (n.relation.name in tmpDeclaredRelations) error(recall(n), Error.DECL_MULTIPLE, n.relation.name)
		tmpDeclaredRelations << n.relation.name

		checkAnnotations(n.annotations, [CONSTRUCTOR, FUNCTIONAL, INPUT, OUTPUT], "Declarations")
		checkArity(n.relation.name, n.types.size(), n)

		if (CONSTRUCTOR in n.annotations && !(n.relation instanceof Constructor))
			error(recall(n), Error.CONSTR_NON_FUNC, n.relation.name)
		if (n.relation instanceof Constructor && !(CONSTRUCTOR in n.annotations))
			error(recall(n), Error.FUNC_NON_CONSTR, n.relation.name)

		n.types.findAll { !it.isPrimitive() }
				.findAll { !(it in typeInfoActor.allTypes) }
				.each { error(recall(it), Error.TYPE_UNKNOWN, it.name) }
	}

	void enter(TypeDeclaration n) {
		if (n.type.name in tmpDeclaredTypes) error(recall(n), Error.DECL_MULTIPLE, n.type.name)
		tmpDeclaredTypes << n.type.name

		checkAnnotations(n.annotations, [INPUT, OUTPUT, TYPE, TYPEVALUES], "Type")
	}

	void enter(Rule n) {
		checkAnnotations(n.annotations, [PLAN], "Rule")

		def varsInHead = relInfoActor.vars[n.head]
		def varsInBody = relInfoActor.vars[n.body]
		def conVars = conInfoActor.constructedVars[n]
		varsInHead.findAll { !(it in varsInBody) && !(it in conVars) }
				.each { error(recall(n), Error.VAR_UNKNOWN, it.name) }

		varsInBody.findAll { it.name != "_" }
				.findAll { !(it in varsInHead) }
				.findAll { varsInBody.count(it) == 1 }
				.each { warn(recall(n), Error.VAR_UNUSED, it.name) }

		asElements(n.head)
				.findAll { it instanceof Relation }
				.collect { it as Relation }
				.findAll { rel -> typeInfoActor.allTypes.find { it.name == rel.name } }
				.each { error(recall(n), Error.TYPE_RULE, it.name) }
	}

	void enter(ConstructionElement n) {
		if (!(n.type in typeInfoActor.allTypes)) error(recall(n), Error.TYPE_UNKNOWN, n.type.name)

		def baseType = conInfoActor.constructorBaseType[n.constructor.name]
		if (!baseType)
			error(recall(n), Error.CONSTR_UNKNOWN, n.constructor.name)
		if (n.type != baseType && !(baseType in typeInfoActor.superTypesOrdered[n.type]))
			error(recall(n), Error.CONSTR_TYPE_INCOMP, n.constructor.name, n.type.name)
	}

	void enter(Constructor n) { if (!inDecl) checkArity(n.name, n.arity, n) }

	void enter(Relation n) { if (!inDecl) checkArity(n.name, n.arity, n) }

	def checkArity(String name, int arity, IVisitable n) {
		def prevArity = arities[name]
		if (prevArity && prevArity != arity) error(recall(n), Error.REL_ARITY, name)
		if (!prevArity) arities[name] = arity
	}

	static def checkAnnotations(Set<Annotation> annotations, List<Annotation> allowedAnnotations, String kind) {
		annotations
				.findAll { !(it in allowedAnnotations)}
				.each { error(recall(it), Error.ANNOTATION_INVALID, it, kind) }
		annotations.each { it.validate() }
	}
}
