package org.codesimius.panda.system

import java.text.MessageFormat
import java.text.SimpleDateFormat

class Log {

	static final def DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

	File logFile

	Log(String filename) {
		if (filename) {
			logFile = new File(filename)
			logFile.parentFile.mkdirs()
			logFile.text = ''
		}
	}

	void info(def tag = null, def msg) {
		msg = "${DATE_FORMAT.format(new Date())} $tag: $msg"
		System.out.println msg
		if (logFile) logFile.append "$msg\n"
	}

	private def prob(def tag, def loc = null, Error errorId, Object... values) {
		def rawMsg = "${MessageFormat.format(Error.msgMap[errorId], values)} -- [$errorId]"
		def msg = "${DATE_FORMAT.format(new Date())} $tag: $rawMsg${loc ? "\n$loc" : ""}"
		System.err.println msg
		if (logFile) logFile.append "$msg\n"
		return rawMsg
	}

	void warn(def loc = null, Error errorId, Object... values) {
		prob "WARNING", loc, errorId, values
	}

	void error(def loc = null, Error errorId, Object... values) {
		def msg = prob "ERROR", loc, errorId, values
		throw new PandaException(msg, errorId)
	}

	void error(Exception e) {
		def msg = e.message
		System.err.println msg
		if (logFile) logFile.append "$msg\n"
	}
}
