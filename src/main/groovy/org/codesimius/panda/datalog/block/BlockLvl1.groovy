package org.codesimius.panda.datalog.block

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.codesimius.panda.datalog.IVisitable

@Canonical
@EqualsAndHashCode(includes = "name")
@ToString(includePackage = false)
class BlockLvl1 implements IVisitable {

	String name
	String superComponent
	List<String> parameters = []
	List<String> superParameters = []
	BlockLvl0 datalog = new BlockLvl0()
}