package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.InfoCollectionVisitingActor
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

class InitializingTransformer extends DummyTransformer {

	// Info collection actor for original program
	InfoCollectionVisitingActor infoActor
	// Program before initialization
	Program origP
	// Program after initialization (only a global component)
	Program initP
	// Current name used for initialization
	String currInitName
	// Current component being initialized
	Component currComp
	// Relations in a component that have a declaration
	Set<String> haveDeclaration
	// Relations in a component that need a declaration (their stage is @ext)
	Set<String> needDeclaration

	boolean inRuleHead

	InitializingTransformer() { actor = this }

	// Initialize components (add transformed contents in a single component)
	// A component might be visited multiple times (depending on inits)
	// Components with no inits are dropped
	IVisitable visit(Program n) {
		infoActor = new InfoCollectionVisitingActor()
		origP = n
		initP = new Program(new Component())

		origP.accept(infoActor)

		// Initializations
		origP.globalComp.accept(this)
		origP.inits.each { initName, compName ->
			currInitName = initName
			currComp = origP.comps[compName]
			if (!currComp) ErrorManager.error(ErrorId.COMP_UNKNOWN, compName)
			currComp.accept(this)
		}

		// Propagation rules
		origP.props.each { prop ->
			if (!origP.inits[prop.fromId])
				ErrorManager.error(ErrorId.COMP_UNKNOWN, prop.fromId)
			if (!origP.inits[prop.toId] && prop.toId)
				ErrorManager.error(ErrorId.COMP_UNKNOWN, prop.toId)

			// fromId is the component name after initialization
			// fromCompTemplate is the component before initialization
			def fromCompTemplate = origP.comps[origP.inits[prop.fromId]]

			def declaredRelations = infoActor.declaringAtoms[fromCompTemplate]

			// If preds is null then "*" was used
			def toPropagate = (prop.preds ?
					prop.preds.collect { p ->
						def relation = declaredRelations.find { it.name == p }
						if (!relation)
							ErrorManager.error(ErrorId.REL_UNKNOWN, p)
						return relation
					} :
					declaredRelations) as Set

			toPropagate.each { rel ->
				// Propagate to global scope and relation already declared there
				if (!prop.toId && rel in infoActor.declaringAtoms[origP.globalComp])
					ErrorManager.error(ErrorId.DEP_GLOBAL, rel.name)

				def head = new LogicalElement(rename(rel, prop.toId, prop.toId != null))
				def body = new LogicalElement(rename(rel, prop.fromId, false))
				initP.globalComp.rules << new Rule(head, body)
			}
		}

		return initP
	}

	void enter(Component n) {
		haveDeclaration = [] as Set
		needDeclaration = [] as Set
	}

	Component exit(Component n, Map<IVisitable, IVisitable> m) {
		n.declarations.each { initP.globalComp.declarations << (m[it] as Declaration) }
		n.rules.each { initP.globalComp.rules << (m[it] as Rule) }

		needDeclaration?.findAll { !(it in haveDeclaration) }?.each {
			ErrorManager.error(ErrorId.REL_NO_DECL_REC, it)
		}
		null
	}

	Declaration exit(Declaration n, Map<IVisitable, IVisitable> m) {
		mayHaveDecl(n)
		new Declaration(m[n.atom] as Relation, n.types.collect { m[it] as Relation }, n.annotations)
	}

	// Override to keep track of when in rule's head
	IVisitable visit(Rule n) {
		actor.enter(n)
		inRuleHead = true
		m[n.head] = n.head.accept(this)
		inRuleHead = false
		if (n.body) m[n.body] = n.body.accept(this)
		return actor.exit(n, m)
	}

	Rule exit(Rule n, Map<IVisitable, IVisitable> m) {
		new Rule(m[n.head] as LogicalElement, m[n.body] as LogicalElement, n.annotations)
	}

	AggregationElement exit(AggregationElement n, Map<IVisitable, IVisitable> m) {
		new AggregationElement(m[n.var] as VariableExpr, m[n.predicate] as Predicate, m[n.body] as IElement)
	}

	ComparisonElement exit(ComparisonElement n, Map<IVisitable, IVisitable> m) {
		new ComparisonElement(m[n.expr] as BinaryExpr)
	}

	GroupElement exit(GroupElement n, Map<IVisitable, IVisitable> m) {
		new GroupElement(m[n.element] as IElement)
	}

	LogicalElement exit(LogicalElement n, Map<IVisitable, IVisitable> m) {
		new LogicalElement(n.type, n.elements.collect { m[it] as IElement })
	}

	NegationElement exit(NegationElement n, Map<IVisitable, IVisitable> m) {
		new NegationElement(m[n.element] as IElement)
	}

	Constructor exit(Constructor n, Map<IVisitable, IVisitable> m) {
		def (String newName, String newStage) = rename(n)
		def f = new Functional(newName, newStage, n.keyExprs, n.valueExpr)
		def (String newTypeName, _) = rename(n.type)
		new Constructor(f, new Type(newTypeName, n.type.exprs.first()))
	}

	Type exit(Type n, Map<IVisitable, IVisitable> m) {
		def (String newName, _) = rename(n)
		new Type(newName, n.exprs.first())
	}

	Functional exit(Functional n, Map<IVisitable, IVisitable> m) {
		checkExt(n)
		mayNeedDecl(n)
		def (String newName, String newStage) = rename(n)
		new Functional(newName, newStage, n.keyExprs, n.valueExpr)
	}

	Predicate exit(Predicate n, Map<IVisitable, IVisitable> m) {
		checkExt(n)
		mayNeedDecl(n)
		def (String newName, String newStage) = rename(n)
		new Predicate(newName, newStage, n.exprs)
	}

	Primitive exit(Primitive n, Map<IVisitable, IVisitable> m) { n }

	BinaryExpr exit(BinaryExpr n, Map<IVisitable, IVisitable> m) { n }

	ConstantExpr exit(ConstantExpr n, Map<IVisitable, IVisitable> m) { n }

	GroupExpr exit(GroupExpr n, Map<IVisitable, IVisitable> m) { n }

	VariableExpr exit(VariableExpr n, Map<IVisitable, IVisitable> m) { n }

	def rename(Relation r) {
		// Global Component
		if (!currInitName)
			return [rename(r, currInitName, r.stage == "@ext").name, null]

		def declared = infoActor.declaringAtoms[currComp].collect { it.name }

		if (r.stage == "@ext")
			return [rename(r, currInitName, true).name, null]
		else if (r.name in declared)
			return [rename(r, currInitName, false).name, null]
		else
			return [r.name, null]
	}

	static def rename(Relation n, String initName, boolean withExt) {
		def prefix = initName ? "$initName:" : ""
		def suffix = withExt ? ":__eXt" : ""
		def name = "$prefix${n.name}$suffix"
		if (n instanceof Functional)
			return new Functional(name, null, n.keyExprs, n.valueExpr)
		else if (n instanceof Predicate)
			return new Predicate(name, null, n.exprs)
		else throw new RuntimeException()
	}

	def mayHaveDecl(Declaration n) {
		// Ignore declarations in global scope
		if (currComp) haveDeclaration << n.atom.name
	}

	def mayNeedDecl(Relation n) {
		// Ignore relations in global scope or without an "@ext" stage
		if (currComp && n.stage == "@ext") needDeclaration << n.name
	}

	def checkExt(Relation r) {
		def loc = SourceManager.instance.recall(r)
		// @ext is allowed in the rule's head only in global space
		if (inRuleHead && currInitName && r.stage == "@ext")
			ErrorManager.error(loc, ErrorId.REL_EXT_HEAD, r.name)
	}
}