package org.clyze.deepdoop.system

import java.text.MessageFormat

enum ErrorId {
	ID_IN_USE,
	SUFFIX_RESERVED,
	COMP_UNKNOWN,
	ANNOTATION_INVALID,
	ANNOTATION_INVALID_ARG,
	ANNOTATION_MISSING_ARG,
	ANNOTATION_MISTYPED_ARG,
	ANNOTATION_NON_EMPTY,
	ANNOTATION_MULTIPLE,
	DECL_MULTIPLE,
	DECL_MALFORMED,
	TYPE_UNKNOWN,
	TYPE_UNSUPP,
	TYPE_FIXED,
	TYPE_INCOMP,
	TYPE_INFERENCE_FAIL,
	TYPE_RULE,
	REL_UNKNOWN,
	REL_NO_DECL_REC,
	REL_ARITY,
	REL_EXT_HEAD,
	VAR_UNKNOWN,
	VAR_UNUSED,
	VAR_MULTIPLE_CONSTR,
	CONSTR_UNKNOWN,
	CONSTR_RULE,
	CONSTR_INCOMP,
	CONSTR_NON_FUNC,
	CONSTR_EXT_HEAD,
	CONSTR_RULE_CYCLE,
	REFMODE_ARITY,
	REFMODE_KEY,

	MULTIPLE_ENT_DECLS,
	DEP_CYCLE,
	DEP_GLOBAL,
	CMD_DIRECTIVE,
	CMD_EVAL,
	CMD_RULE,
	CMD_NO_DECL,
	CMD_NO_IMPORT,

	static Map<ErrorId, String> msgMap
	static {
		msgMap = new EnumMap<>(ErrorId.class)
		msgMap[ID_IN_USE] = "Id `{0}` already in use to initialize a component"
		msgMap[SUFFIX_RESERVED] = "Suffix `__pArTiAl` is reserved and cannot appear in a relation name"
		msgMap[COMP_UNKNOWN] = "Unknown component `{0}`"
		msgMap[ANNOTATION_INVALID] = "Invalid annotation `{0}` for `{1}`"
		msgMap[ANNOTATION_INVALID_ARG] = "Invalid argument `{0}` for annotation `{1}`"
		msgMap[ANNOTATION_MISSING_ARG] = "Missing mandatory argument `{0}` for annotation `{1}`"
		msgMap[ANNOTATION_MISTYPED_ARG] = "Type mismatch ({0} instead of {1}) for argument `{2}` of annotation `{3}`"
		msgMap[ANNOTATION_NON_EMPTY] = "Annotation `{0}` takes no arguments"
		msgMap[ANNOTATION_MULTIPLE] = "Annotation `{0}` appears more than once in clause"
		msgMap[DECL_MULTIPLE] = "Multiple declarations for relation `{0}`"
		msgMap[DECL_MALFORMED] = "Malformed declaration"
		msgMap[TYPE_UNKNOWN] = "Unknown type `{0}`"
		msgMap[TYPE_UNSUPP] = "Unsupported type `{0}` (currently)"
		msgMap[TYPE_FIXED] = "Declared type `{0}` (at index {1}) for relation `{2}`"
		msgMap[TYPE_INCOMP] = "Incompatible types for relation `{0}` (at index {1})"
		msgMap[TYPE_INFERENCE_FAIL] = "Type inference was inconclusive: cannot reach fixpoint"
		msgMap[TYPE_RULE] = "Type `{0}` used as a normal relation in rule head"
		msgMap[REL_UNKNOWN] = "Unknown relation `{0}` used in propagation"
		msgMap[REL_NO_DECL_REC] = "Undeclared relation `{0}` used with `@ext`"
		msgMap[REL_ARITY] = "Inconsistent arity for relation `{0}`"
		msgMap[REL_EXT_HEAD] = "Relation `{0}` used with `@ext` in rule head, inside a component"
		msgMap[VAR_UNKNOWN] = "Unknown var `{0}`"
		msgMap[VAR_UNUSED] = "Unused var `{0}`"
		msgMap[VAR_MULTIPLE_CONSTR] = "Var `{0}` constructed by multiple constructors"
		msgMap[CONSTR_UNKNOWN] = "Unknown constructor `{0}`"
		msgMap[CONSTR_RULE] = "Constructor `{0}` used as a normal relation in rule head"
		msgMap[CONSTR_INCOMP] = "Constructor `{0}` used with incompatible type `{1}`"
		msgMap[CONSTR_NON_FUNC] = "Constructor `{0}` must be a functional relation"
		msgMap[CONSTR_EXT_HEAD] = "Constructor `{0}` cannot be used with `@ext` in rule head"
		msgMap[CONSTR_RULE_CYCLE] = "Constructor `{0}` appears in a cycle in rule head"
		msgMap[REFMODE_ARITY] = "Refmode `{0}` arity must be 2"
		msgMap[REFMODE_KEY] = "Refmode `{0}` key argument must be primitive"

		msgMap[MULTIPLE_ENT_DECLS] = "Multiple declarations for Type `{0}` in previous components"
		msgMap[DEP_CYCLE] = "Cycle detected in the dependency graph of components"
		msgMap[DEP_GLOBAL] = "Reintroducing relation `{0}` to global space"
		msgMap[CMD_DIRECTIVE] = "Invalid directive in command block `{0}`"
		msgMap[CMD_EVAL] = "EVAL property already specified in command block `{0}`"
		msgMap[CMD_RULE] = "Normal rules are not supported in a command block"
		msgMap[CMD_NO_DECL] = "Relation `{0}` is imported but has no declaration"
		msgMap[CMD_NO_IMPORT] = "Relation `{0}` is declared but not imported"
	}

	static String idToMsg(ErrorId errorId, Object[] values) {
		MessageFormat.format(msgMap.get(errorId), values)
	}
}
