package com.petukhovsky.jvaluer

import com.petukhovsky.jvaluer.api.request.SubmissionInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * Created by arthur on 21.02.17.
 */

val timeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.UK)
        .withZone(ZoneId.of("Europe/Minsk"))

@Controller
class ViewController @Autowired constructor(
        val testingService: TestingService,
        val subRepo: SubmitRepository
){
    @GetMapping("/")
    fun index(model: Model, principal: Principal?): String {
        if (principal != null) {
            model["signedIn"] = true
            model["username"] = principal.name
        } else {
            model["signedIn"] = false
        }
        //SecurityContextHolderAwareRequestWrapper.isUserInRole("USER_ROLE")
        return "index"
    }

    @GetMapping("/tasks")
    fun tasks(model: Model): String {
        model["tasks"] = Tasks.all.map { TaskLink("/task/${it.id}", it.model.name, it.id) }
        return "tasks"
    }

    //[{"time": "02:28", "status": "OK", "points": 100,
    // "href": "/submission.html", "timeUsed": "1903ms", "lang": "cpp", "num": 123}]
    @GetMapping("/task/{id}")
    fun task(model: Model, @PathVariable("id") id: String, principal: Principal?): String {
        val user = principal?.name

        val task = Tasks.getTaskById(id)!!
        val subs: Array<SubInfo> = if (user == null) arrayOf()
            else subRepo.findByAuthor(user)
                                    .filter { it.task == id }
                                    .map { it.toInfo() }
                                    .reversed()
                                    .toTypedArray()
        model["subs"] = objectMapper.writeValueAsString(subs).replace('\'', '`')
        model["name"] = task.model.name
        model["statement"] = task.htmlStatement
        model["id"] = task.id
        model["nsignedIn"] = principal == null
        return "task"
    }

    @GetMapping("/status")
    fun status(model: Model): String {
        val subs = subRepo.findAll()
                .takeLast(50)
                .map { it.toInfo() }
                .reversed()
                .toTypedArray()
        model["subs"] = objectMapper.writeValueAsString(subs).replace('\'', '`')
        return "status"
    }

    @GetMapping("/results")
    fun result(model: Model): String {
        val result = testingService.buildTable()
        model["result"] = objectMapper.writeValueAsString(result).replace('\'', '`')
        return "result"
    }

    @GetMapping("/submission/{id}")
    fun submission(model: Model, @PathVariable("id") id: Int, principal: Principal?): String {
        val user = principal?.name
        val sub = subRepo.findByNum(id)!!
        if (sub.author != user) throw IllegalAccessException("Недостаточно прав")

        val tests = sub.tests.map { TestResultWithName(it.key, it.value) }

        val subInfo = sub.toInfo(personal = true)

        model["subInfo"] = objectMapper.writeValueAsString(subInfo).replace('\'', '`')
        model["tests"] = objectMapper.writeValueAsString(tests).replace('\'', '`')
        model["subNum"] = id.toString()

        return "submission"
    }

    @PostMapping("/task/{id}/submit")
    fun submitTask(
            @PathVariable("id") id: String,
            principal: Principal?,
            @RequestParam("lang") lang: String,
            @RequestParam("src") src: String
    ): ResponseEntity<String> {
        val task = Tasks.getTaskById(id)!!
        val user = principal!!.name
        testingService.submit(user, id, lang, src)
        return ResponseEntity.ok("All ok")
    }
}

class ResultTable(
        val tasksCount: Int,
        val tasks: Array<String>,
        val result: Array<ResultRow>
)

class ResultRow(
    val user: String,
    val row: Array<Double>,
    val sum: Double
)

data class SubInfo(
        val time: String,
        val status: String,
        val points: Double,
        val timeUsed: String,
        val lang: String,
        val num: Int,
        val href: String,
        val taskId: String,
        val author: String,
        val src: String,
        val log: String
)

fun Submit.toInfo(personal: Boolean = false): SubInfo {
    return SubInfo(
            timeFormatter.format(this.submitted),
            this.status,
            this.points,
            this.maxTime.toString() + "ms",
            this.lang!!,
            this.num,
            "/submission/${this.num}",
            this.task,
            this.author,
            if (personal) this.src else "",
            if (personal) this.compilationLog else ""
    )
}

data class TaskLink(
        val href: String,
        val name: String,
        val id: String
)