package org.clyze.deepdoop.actions.code

import org.clyze.deepdoop.actions.*
import org.clyze.deepdoop.actions.tranform.TypeInferenceTransformer
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.GroupElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Result

import java.nio.file.Files
import java.nio.file.Paths

import static org.clyze.deepdoop.datalog.element.ComparisonElement.TRIVIALLY_TRUE
import static org.clyze.deepdoop.datalog.element.LogicalElement.LogicType.AND

class DefaultCodeGenerator extends DefaultVisitor<String> implements TDummyActor<String> {

	protected File outDir
	protected File currentFile

	protected TypeInfoVisitingActor typeInfoActor = new TypeInfoVisitingActor()
	protected RelationInfoVisitingActor relInfoActor = new RelationInfoVisitingActor()
	protected ConstructionInfoVisitingActor constructionInfoActor = new ConstructionInfoVisitingActor()
	protected TypeInferenceTransformer typeInferenceTransformer = new TypeInferenceTransformer(typeInfoActor, relInfoActor)

	List<Result> results = []

	DefaultCodeGenerator(File outDir) {
		actor = this
		this.outDir = outDir
	}

	DefaultCodeGenerator(String outDir) { this(new File(outDir)) }

	//String exit(BlockLvl2 n, Map m) { null }

	//String exit(BlockLvl1 n, Map m) { null }

	//String exit(BlockLvl0 n, Map m) { null }

	//String exit(RelDeclaration n, Map m) { null }

	//String exit(TypeDeclaration n, Map m) { null }

	//String exit(Rule n, Map m) { null }

	//String exit(AggregationElement n, Map m) { null }

	String exit(ComparisonElement n, Map m) { n == TRIVIALLY_TRUE ? "true" : m[n.expr] }

	//String exit(ConstructorElement n, Map m) { null }

	String exit(GroupElement n, Map m) { "(${m[n.element]})" }

	String exit(LogicalElement n, Map m) {
		n.elements.findAll { m[it] }.collect { m[it] }.join(n.type == AND ? ", " : "; ")
	}

	String exit(NegationElement n, Map m) { "!${m[n.element]}" }

	//String exit(Relation n, Map m) { null }

	//String exit(Constructor n, Map m) { null }

	//String exit(Type n, Map m) { null }

	String exit(BinaryExpr n, Map m) { "${m[n.left]} ${n.op} ${m[n.right]}" }

	String exit(ConstantExpr n, Map m) { n.value as String }

	String exit(GroupExpr n, Map m) { "(${m[n.expr]})" }

	String exit(VariableExpr n, Map m) { n.name }

	protected def createUniqueFile(String prefix, String suffix) {
		Files.createTempFile(Paths.get(outDir.name), prefix, suffix).toFile()
	}

	protected void emit(String data) { write currentFile, data }

	protected static void write(File file, String data) { file << data << "\n" }
}
