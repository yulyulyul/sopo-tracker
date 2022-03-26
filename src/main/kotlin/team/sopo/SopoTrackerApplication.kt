package team.sopo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SopoTrackerApplication

fun main(args: Array<String>) {
	runApplication<SopoTrackerApplication>(*args)
}
