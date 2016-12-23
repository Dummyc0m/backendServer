package cn.com.guardiantech.classroom.server.console.consoleManager

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ConsoleMonitor(val targetStream: InputStream) : Runnable {

    val reader: BufferedReader = BufferedReader(InputStreamReader(targetStream))

    var running = true

    fun terminate() {
        running = false;
    }

    override fun run() {
        while (running) {
            val line = reader.readLine()
            ConsoleManager.processCommand(line)
        }
    }
}