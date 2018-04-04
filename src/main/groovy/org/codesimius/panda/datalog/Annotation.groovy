package org.codesimius.panda.datalog

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.system.Error
import org.codesimius.panda.system.SourceManager

import static org.codesimius.panda.datalog.expr.ConstantExpr.Type.*
import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

@EqualsAndHashCode(includes = "kind")
@ToString(includePackage = false)
class Annotation {

	String kind
	Map<String, ConstantExpr> args

	Annotation(String name, Map<String, ConstantExpr> args = [:]) {
		name = name.toUpperCase()
		if (!VALIDATORS.containsKey(name)) error(recall(this), Error.ANNOTATION_UNKNOWN, name)
		this.kind = name
		this.args = args
	}

	String toString() { "@$kind${args ? "$args " : ""}" }

	void validate() { VALIDATORS[kind]?.call(this) }

	static def NO_ARGS_VALIDATOR = { Annotation a ->
		if (!a.args.isEmpty()) error(SourceManager.instance.recall(a), Error.ANNOTATION_NON_EMPTY, a.kind)
	}

	static def MANDATORY_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> mandatory ->
		mandatory.findAll { argName, type -> !a.args[argName] }.each {
			error(recall(a), Error.ANNOTATION_MISSING_ARG, it, a.kind)
		}
		mandatory.findAll { argName, type -> a.args[argName].type != type }.each { argName, type ->
			error(recall(a), Error.ANNOTATION_MISTYPED_ARG, a.args[argName].type, type, argName, a.kind)
		}
	}

	static def OPTIONAL_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> optional ->
		a.args.findAll { argName, value -> !optional[argName] }.each {
			error(recall(a), Error.ANNOTATION_INVALID_ARG, it.key, a.kind)
		}
		a.args.findAll { argName, value -> optional[argName] != value.type }.each { argName, value ->
			error(recall(a), Error.ANNOTATION_MISTYPED_ARG, a.args[argName].type, optional[argName], argName, a.kind)
		}
	}

	static Map<String, Closure> VALIDATORS = [
			"CONSTRUCTOR": NO_ARGS_VALIDATOR,
			"FUNCTIONAL" : NO_ARGS_VALIDATOR,
			"INPUT"      : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, [filename: STRING, delimeter: STRING])
			},
			"NAMESPACE"  : { Annotation a ->
				MANDATORY_VALIDATOR.call(a, [v: STRING])
			},
			"OUTPUT"     : NO_ARGS_VALIDATOR,
			"PLAN"       : { Annotation a ->
				MANDATORY_VALIDATOR.call(a, [plan: STRING])
				OPTIONAL_VALIDATOR.call(a, [plan: STRING])
			},
			"TYPE"       : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, [capacity: INTEGER, defaultConstructor: BOOLEAN])
			},
			"TYPEVALUES" : {},
	]

	static final CONSTRUCTOR = new Annotation("CONSTRUCTOR")
	static final FUNCTIONAL = new Annotation("FUNCTIONAL")
	static final INPUT = new Annotation("INPUT")
	static final NAMESPACE = new Annotation("NAMESPACE")
	static final OUTPUT = new Annotation("OUTPUT")
	static final PLAN = new Annotation("PLAN")
	static final TYPE = new Annotation("TYPE")
	static final TYPEVALUES = new Annotation("TYPEVALUES")
}
